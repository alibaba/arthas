
## FAQ


> 不在本列表里的问题，请到issue里搜索。 [https://github.com/alibaba/arthas/issues](https://github.com/alibaba/arthas/issues)

##### Arthas attach之后对原进程性能有多大的影响

[https://github.com/alibaba/arthas/issues/44](https://github.com/alibaba/arthas/issues/44)


##### 怎么以`json`格式查看结果

```bash
options json-format true
```

更多参考 [options](options.md)


##### Arthas能否跟踪 native 函数

不能。


##### 能不能查看内存里某个变量的值

不能。但可以用一些技巧，用`tt`命令拦截到对象，或者从静态函数里取到对象。