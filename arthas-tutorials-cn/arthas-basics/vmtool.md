通过 [vmtool 命令](https://arthas.aliyun.com/doc/vmtool.html)，可以搜索内存对象。

## 获取 class 实例

`vmtool --action getInstances --className demo.MathGame --limit 10`{{exec}}

## 调用实例方法

### 查看 class 源码

`jad demo.MathGame`{{exec}}

### 调用实例方法

`vmtool --action getInstances --className demo.MathGame --express 'instances[0].primeFactors(3)' -x 3`{{exec}}
