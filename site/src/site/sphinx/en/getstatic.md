getstatic
=========

Check the static fields of classes conveniently as `getstatic class_name field_name`

Tip: if the *field* is a composite object, you can even use [`OGNL`](https://en.wikipedia.org/wiki/OGNL) to traverse, filter and access the internal properties of it.

E.g. suppose `n` is a `Map` and the key is a `Enum` then if you want to get the key with a specific `Enum` value - `STOP` or `a`, you can achieve it as:

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
