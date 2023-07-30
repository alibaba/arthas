通过 [vmtool 命令](https://arthas.aliyun.com/doc/vmtool.html)，可以搜索内存对象。

###  获取class 实例

`vmtool --action getInstances --className demo.MathGame --limit 10`{{exec}}

```bash
$ vmtool --action getInstances --className demo.MathGame --limit 10
@MathGame[][
    @MathGame[demo.MathGame@3349e2d],
]
```

###  调用实例方法 

#### 查看class 源码
`jad demo.MathGame`{{exec}}

#### 调用实例方法 
`vmtool --action getInstances  --className demo.MathGame  --express 'instances[0].primeFactors(3)' -x 3`{{exec}}

```
vmtool --action getInstances  --className demo.MathGame  --express 'instances[0].primeFactors(3)' -x 3
@ArrayList[
    @Integer[3],
]
```
