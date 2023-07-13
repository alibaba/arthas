
下面介绍Arthas里查看`JVM`信息的命令。

### sysprop

`sysprop`{{execute T2}} 可以打印所有的System Properties信息。

也可以指定单个key： `sysprop java.version`{{execute T2}}

也可以通过`grep`来过滤： `sysprop | grep user`{{execute T2}}

可以设置新的value： `sysprop testKey testValue`{{execute T2}}

### sysenv

`sysenv`{{execute T2}} 命令可以获取到环境变量。和`sysprop`命令类似。



### jvm

`jvm`{{execute T2}} 命令会打印出`JVM`的各种详细信息。


### dashboard


`dashboard`{{execute T2}} 命令可以查看当前系统的实时数据面板。

输入 `Q`{{execute T2}} 或者 `Ctrl+C` 可以退出dashboard命令。