

下面使用`vmtool`命令查找jvm对象。


### 查找jvm里的字符串对象

`vmtool --action getInstances --className java.lang.String`{{execute T2}}

```bash
$ vmtool --action getInstances --className java.lang.String
@String[][
    @String[Sorry, deque too big],
    @String[head=%d tail=%d capacity=%d%n],
    @String[elements=%s%n],
    @String[sun/nio/ch/IOVecWrapper],
    @String[40252e37-8a73-4960-807e-3495addd5b08:1620922383791],
    @String[40252e37-8a73-4960-807e-3495addd5b08:1620922383791],
    @String[sun/nio/ch/AllocatedNativeObject],
    @String[sun/nio/ch/NativeObject],
    @String[sun/nio/ch/IOVecWrapper$Deallocator],
    @String[Java_sun_nio_ch_FileDispatcherImpl_writev0],
]
```

### limit参数

> 通过 `--limit`参数，可以限制返回值数量，避免获取超大数据时对JVM造成压力。默认值是10。

所以上面的命令实际上等值于：

```bash
vmtool --action getInstances --className java.lang.String --limit 10
```

如果设置`--limit`为负数，则遍历所有对象。

