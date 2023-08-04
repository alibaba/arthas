下面使用 [vmtool 命令](https://arthas.aliyun.com/doc/vmtool.html) 查找 jvm 对象。

### 查找 jvm 里的字符串对象

`vmtool --action getInstances --className java.lang.String`{{execute T2}}

### limit 参数

> 通过 `--limit`参数，可以限制返回值数量，避免获取超大数据时对 JVM 造成压力。默认值是 10。

所以上面的命令实际上等值于：

`vmtool --action getInstances --className java.lang.String --limit 10`{{exec}}

如果设置`--limit`为负数，则遍历所有对象。
