
> Memory Compiler/内存编译器，编译`.java`文件生成`.class`。


可以通过`-c`/`--classLoaderClass`参数指定classloader，`-d`参数指定输出目录

编译生成`.class`文件之后，可以结合`retransform`命令实现热更新代码。
