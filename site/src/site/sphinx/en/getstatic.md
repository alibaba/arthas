getstatic
=========

Check the static fields of classes conveniently as `getstatic class_name field_name`

If the *field* is a composite object, you can even use [`OGNL`](https://en.wikipedia.org/wiki/OGNL) to traverse, filter and access the internal properties of it.

```
$ getstatic demo.Demo$Counter count
field: count
@AtomicInteger[
    serialVersionUID=@Long[6214790243416807050],
    unsafe=@Unsafe[sun.misc.Unsafe@1f99cf8d],
    valueOffset=@Long[12],
    value=@Integer[2425],

]
Affect(row-cnt:1) cost in 9 ms.

$ getstatic demo.Demo$Counter count 'get()'
field: count
@Integer[2505]
Affect(row-cnt:1) cost in 12 ms.

# a complicated filtering in another demo
$ getstatic com.alibaba.arthas.Test m 'entrySet().iterator.{? #this.key=="a"}'
field: m
@ArrayList[
    @Node[a=aaa],

]
```
