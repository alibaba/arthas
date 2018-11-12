getstatic
=========

Check the static fields of classes conveniently, the usage is `getstatic class_name field_name`.

Tip: if the static field is a complex class, you can even use [`OGNL`](https://en.wikipedia.org/wiki/OGNL) to traverse, filter and access the inner properties of this class.

E.g. suppose `n` is a `Map` and its key is a `Enum`, then you can achieve this if you want to pick the key with a specific `Enum` value:

```bash
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
