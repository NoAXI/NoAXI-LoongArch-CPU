# 命名规范

变量随便命名，开头小写即可

```scala
val linkSignal = WireDefault(false.B)
val do_flush   = WireDefault(false.B)
```

类名开头大写，文件名与类名相同，而且开头大写

```scala
class ForExampleModuleIO extend Bundle {
    val qwq = Input(Bool())
}
class ForExampleModule {
    val io = IO(new ForExampleModuleIO)
}
```

表示moduleAlice<>moduleBob的时候，使用bundle来连接，A在前B在后，后面接一个IO

```scala
class ModuleAliceModuleBobIO {
    val ...
}
```

