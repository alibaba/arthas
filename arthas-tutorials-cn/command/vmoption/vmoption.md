查看，更新 VM 诊断相关的参数

使用 `vmoption -h`{{execute T2}} 查看帮助信息

[vmoption 命令文档](https://arthas.aliyun.com/doc/vmoption.html)

## 使用参考

### 查看所有的 option：

`vmoption`{{execute T2}}

### 查看指定的 option

`vmoption PrintGC`{{execute T2}}

### 更新指定的 option

`vmoption PrintGC true`{{execute T2}}

再使用`vmtool`命令执行强制 GC，则可以在`Terminal 1`看到打印出 GC 日志：

`vmtool --action forceGc`{{execute T2}}

### 配置打印 GC 详情

`vmoption PrintGCDetails true`{{execute T2}}

再使用`vmtool`命令执行强制 GC，则可以在`Terminal 1`看到打印出 GC 详情：

`vmtool --action forceGc`{{execute T2}}
