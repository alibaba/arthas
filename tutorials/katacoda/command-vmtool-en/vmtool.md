


Use the `vmtool` command to find the jvm object.


### Find string objects in jvm

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

### limit parameter

> Through the `--limit` parameter, you can limit the number of return values to avoid pressure on the JVM when obtaining large data. The default value is 10.

So the above command is actually equivalent to:

```bash
vmtool --action getInstances --className java.lang.String --limit 10
```

If you set `--limit` to a negative number, all objects are traversed.
