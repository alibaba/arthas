getstatic
===

通过getstatic命令可以方便的查看类的静态属性。使用方法为`getstatic class_name field_name`


如果该静态属性是一个复杂对象，还可以支持在该属性上通过ognl表示进行遍历，过滤，访问对象的内部属性等操作。

例如，假设n是一个Map，Map的Key是一个Enum，我们想过滤出Map中Key为某个Enum的值，可以写如下命令

```
$ getstatic com.alibaba.arthas.Test n 'entrySet().iterator.{? #this.key.name()=="STOP"}'
field: n
@ArrayList[
    @Node[STOP=bbb],
]
Affect(row-cnt:1) cost in 68 ms.


$ getstatic com.alibaba.arthas.Test m 'entrySet().iterator.{? #this.key=="a"}'
field: m
@ArrayList[
    @Node[a=aaa],
]
```