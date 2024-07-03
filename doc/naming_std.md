# 命名规范

变量使用驼峰命名，开头小写

```scala
val linkSignal = WireDefault(false.B)
```

类名开头大写，文件名与类名相同，而且开头大写

```scala
class ForExampleModuleIO extends Bundle {
    val qwq = Input(Bool())
}
class ForExampleModule extends Module {
    val io = IO(new ForExampleModuleIO)
}
```

表示moduleAlice<>moduleBob的时候，使用bundle来连接，A在前B在后，后面接一个IO

```scala
class ModuleAliceModuleBobIO extends Bundle {
    val ...
}
```

函数小写开头

```scala
def funcForTest(a: UInt): UInt = {
    ...
}
```

