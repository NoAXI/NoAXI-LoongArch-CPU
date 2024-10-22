# NoAXI乱序流水线设计 初期方案

ε＝ε＝ε＝(#>д<)ﾉ

## 概述

乱序 双发射

前端取指深度n为4 (这样才好填满双发的需求)

后端流水线共4条

## 前端

### 1-pre-fetch

1. v_pc发送至tlb，tlb进行翻译 
   - 若是直接地址翻译或直接地址映射，寄存器锁存；(目前实现：p_pc = v_pc)
   - 若是映射地址翻译，给tlb表项发地址

2. v_pc_index发送至iCache，给iCache索引line

3. 进行分支预测
   - 分支预测包含btb, bht, pht
   - asid、pc送入btb判断是否命中，送入bht+pht判断是否预测进行跳转
   - btb使用异步sram维护，直接映射（是否需要拆分到下一级流水线？）

4. 下一拍，pc置为pc_next

   - 默认为pc_next_line
   - 发生跳转预测为btb_target
   - 发生冲刷为bru_target
   (bru_target应该就是预测失败后的纠正跳转或异常跳转)

### 2-fetch

1. tlb返回paddr
2. paddr发送至iCache，判定是否命中
3. 取出n条指令，最早的一条跳转及以后的指令舍弃

我觉得这边sram的行为有待商榷，它似乎在prefetch什么事情都没干...？

### 2.5-instruction buffer

队列实现  
将取出的指令填到IB中，后续的译码从IB中取

### 3-pre-decode

分支预测btb的添加放在这一级

此时需要进一步进行分支预测和流水线冲刷，并对prefetch的btb进行更新。具体而言，分三种情况：

1. 未被记录的无条件跳转指令，但流水线顺序取指，所以一定为预测失败
2. 未被记录的条件跳转指令，暂且认定为需要跳转，则预测失败
3. 被标记为跳转成功的指令，但在此发现不是跳转，则预测失败

当检测到预测失败信号时，清空前两级流水线

同时，这一级也用于标记中断。

### 3.5-decode

用于译码，同顺序单发流水线的写法

这一级我觉得可以和predecode合并

### 4-rename&dispatch

#### rename

本级流水线用于进行寄存器重命名

1. 分配物理寄存器

   从freelist当中取两个空闲寄存器p1, p2作为r1, r2对应的物理寄存器

   freelist接收retire后的物理寄存器编号

2. 使用重命名映射表cRAT进行映射。

   下一拍，置RAT(r1)=p1, RAT(r2)=p2

从commit级接入aRAT数据用于分支预测失败/例外的状态恢复。

至此指令开始占用映射表信息和物理寄存器

#### dispatch

用于分发指令到各个流水线发射缓存当中

与重命名同属于第五级，并行执行。

具体工作如下

1. 根据两条指令的类型，生成对应的目标流水线编号

2. 查询ROB，获得两个待分配的ROB编号

3. 将指令信息记录在对应的ROB表项当中

至此指令开始占用ROB表项，进入后端处理




## 分支预测
1. 根据pc判断是否为跳转指令
    - 利用`BHT`表，添加`valid`项，表示pc对应的是否为跳转指令，在更新`BHT`表的时候更新`valid`
    - 读取`BHT`表，看该pc是否为跳转. 利用`BHT(Branch History Table)`记录pc对应的跳转历史`BHR(Branch History Register)`
    - 更新时机：`commit`时
    - TODO:可以使用哈希(如异或)映射pc
3. 利用`PHT(Pattern History Table)`记录`BHR`对应的跳转历史
    - 暂定所有分支指令共用一个`PHT`
4. *基于全局历史`GHR(Global History Register)`的分支预测
    - 可以理解成`BHT`中只有一个表项，此时`BHT`表就变成了`GHR`，这一项记录所有跳转指令的历史
5. *竞争的分支预测
    - 对于一个分支指令，记录其使用`BHR`和使用`GHR`的成功失败信息，如果使用某一个历史失败两次，则转用另一个历史  
6. 对于直接跳转类型和`CALL`，利用`BTB(Branch Target Buffer)`记录pc对应的跳转地址，同时`BTB`兼顾判断跳转类型(CALL,Return,其他)的职责
    - 对`pc`进行哈希运算
    - 如果缺失，顺序取指
    - 认为一个`pc`哈希映射后的结果仅存在一个跳转，若存在多个则会降低预测成功率(因此使用直接相连)

    | Valid | Tag   | BTA   | Br_type |
    |:-----:|:-----:|:-----:|:-------:|
    | 1 b   | 1 b   | 1 b   | 2 b     |

7. 对于间接跳转类型(`Return`)，利用`RAS(Return Address Stack)`记录最近执行的`CALL`指令的下一条指令地址，并使用栈顶值作为预测跳转地址
    - `RAS`满时，对`RAS`使用循环更新，即再从底向上开始更新
    - *TODO:对`RAS`的每一项添加一个计数属性，表示该地址出现的次数


## 后端

后端定义了四个独立的单发射流水线，其中访存流水线强制要求为顺序流水

### 5-issue

这里定义了四个发射队列

对于单个发射队列，每个周期最多接收/发射一条指令

访存流水线的发射队列为正常的顺序发射，因此只需要管头尾指针的fire即可

其余三条流水线都是乱序发射，采用压缩方式存储

size：arithmetic: 6? or 8?;  muldiv: 4;  memory: 8

### 执行流水线

执行级为6~7级，为独立的四条流水线

2条算术执行流水线，1条乘除法执行流水线，1条访存执行流水线

特权指令放在乘除法，在commit的时候再读写csr

#### 唤醒

我们认为每个流水线的最后一级才获得数据

推测唤醒需要放置在获得数据的上一级

因此算术、乘除在0-read reg处唤醒，访存在2-memhit处唤醒

### 6-read-reg

这一级流水线在每条流水线当中都独立存在

从物理寄存器当中读出数值

寄存器堆应该对于每一条流水线都分配一个读接口

算术和乘除进行唤醒

### 7a-arithmetic-pipeline

共2条

只有一级，用于执行算术指令运算

### 7b-muldiv-pipeline

仅用于乘除指令的执行

均使用阻塞执行，因此目前只有一级

后续可能会考虑自行实现乘法部件，添加流水级

考虑把nop流到这一流水级

### 7c-memory-pipeline

以访存为主的流水线，包含了所有与读写存储器相关的 / 必须顺序执行的操作

具体而言，包含了 访存指令、需要读寄存器的分支指令

并且保证所有的例外都在这一级报出，这样可以控制例外的行为与分支行为相同。

有多级

#### 7c-1-memaddr

访存指令：计算vaddr，并发送vaddr至tlb-hit和tag-sram；

分支指令：判断是否预测失败，若失败则令流水线后续指令阻塞，直到本指令提交为止

#### 7c-2-memhit

tlb-hit送入tlb-read，tlb-read返回paddr，判断是否产生tlb例外

tag-sram返回tag，判断命中，若未命中进行replace cache line

例外将在1、2两级触发，一旦触发，立马阻塞流水线

发送读信号到存储器

进行唤醒

#### 7c-3-memdata

对于read，获得sram返回的read数据

对于write，写sram或加入写缓存

其余读写指令同理

### 8-writeback

这一级用于写物理寄存器，并更新rob当中的状态为complete

### 9-commit

这一级用于进行最终的提交

2. 解除对于上一个物理寄存器的占用（opreg），并将retire后的寄存器编号插入到空闲寄存器列表freelist
3. 更新aRAT的映射关系（aRAT[areg] -> preg），aRAT当中存储的是 由已经提交的指令构成的、不涉及推测态指令的映射表
4. 当发生流水线清空的时候，通过aRAT对cRAT进行恢复，但是，不清空原本流水线中指令对应的rob表项，不恢复空闲寄存器列表，仅置流水线中的指令valid=0，通过后续指令的提交来进行这两项的暴力恢复（换句话来说，仅对RAT进行恢复，剩余进行WALK恢复）

关于ROB，我暂且认定它的size=32（可能改成24？），它的表项包含以下几项

1. complete 指令是否完成，只有完成才能进行提交
2. areg 逻辑寄存器下标
3. preg 占用的物理寄存器下标
4. opreg 替换掉的旧物理寄存器下标
5. pc 对应的pc值（用于debug输出）
6. data 被写入preg的值（似乎用不到，也仅用于debug输出？）

只有当complte=true的时候，后面的数据才被写入，其余时间被寄存器锁存在各个流水级当中
