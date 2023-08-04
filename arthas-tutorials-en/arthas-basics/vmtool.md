The [vmtool command](https://arthas.aliyun.com/en/doc/vmtool.html) can search object in JVM.

`vmtool --action getInstances --className demo.MathGame --limit 10`{{exec}}

## Calling an instance method

### Viewing the source code of a class

`jad demo.MathGame`{{exec}}

### Calling an instance method

`vmtool --action getInstances --className demo.MathGame --express 'instances[0].primeFactors(3)' -x 3`{{exec}}
