下面介绍 Arthas 里查看 `JVM` 信息的命令。

### [sysprop](https://arthas.aliyun.com/doc/sysprop.html)

`sysprop`{{execute T2}} 可以打印所有的 System Properties 信息。

也可以指定单个 key： `sysprop java.version`{{execute T2}}

也可以通过`grep`来过滤： `sysprop | grep user`{{execute T2}}

可以设置新的 value： `sysprop testKey testValue`{{execute T2}}

### [sysenv](https://arthas.aliyun.com/doc/sysenv.html)

`sysenv`{{execute T2}} 命令可以获取到环境变量。和`sysprop`命令类似。

### [jvm](https://arthas.aliyun.com/doc/jvm.html)

`jvm`{{execute T2}} 命令会打印出`JVM`的各种详细信息。

### [dashboard](https://arthas.aliyun.com/doc/dashboard.html)

`dashboard`{{execute T2}} 命令可以查看当前系统的实时数据面板。

输入 `Q`{{exec interrupt}} 或者 `Ctrl+C`{{exec interrupt}} 可以退出 dashboard 命令。
