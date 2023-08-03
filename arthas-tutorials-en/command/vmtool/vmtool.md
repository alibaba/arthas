Use the [vmtool command](https://arthas.aliyun.com/en/doc/vmtool.html) to find the jvm object.

### Find string objects in jvm

`vmtool --action getInstances --className java.lang.String`{{execute T2}}

### limit parameter

> Through the `--limit` parameter, you can limit the number of return values to avoid pressure on the JVM when obtaining large data. The default value is 10.

So the above command is actually equivalent to:

` vmtool --action getInstances --className java.lang.String --limit 10`{{exec}}

If you set `--limit` to a negative number, all objects are traversed.
