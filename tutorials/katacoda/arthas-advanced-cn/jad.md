
可以通过 `jad` 命令来反编译代码：

`jad com.example.demo.arthas.user.UserController`{{execute T2}}


通过`--source-only`参数可以只打印出在反编译的源代码：

`jad --source-only com.example.demo.arthas.user.UserController`{{execute T2}}
