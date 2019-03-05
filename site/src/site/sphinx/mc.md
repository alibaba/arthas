mc
===

> Memory Compiler/内存编译器，编译`.java`文件生成`.class`。

```bash
mc /tmp/Test.java
```

可以通过`-c`参数指定classloader：

```bash
mc -c 327a647b /tmp/Test.java
```

可以通过`-d`命令指定输出目录：

```bash
mc -d /tmp/output /tmp/ClassA.java /tmp/ClassB.java
```

编译生成`.class`文件之后，可以结合[redefine](redefine.md)命令实现热更新代码。

> 注意，mc命令有可能失败。如果编译失败可以在本地编译好`.class`文件，再上传到服务器。具体参考[redefine](redefine.md)命令说明。