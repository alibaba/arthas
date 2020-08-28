getstatic
=========

[`getstatic` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-getstatic)

* It is recommended to use the [OGNL] (ognl.md) command, which will be more flexible.

Check the static fields of classes conveniently, the usage is `getstatic class_name field_name`.

```bash
$ getstatic demo.MathGame random
field: random
@Random[
    serialVersionUID=@Long[3905348978240129619],
    seed=@AtomicLong[120955813885284],
    multiplier=@Long[25214903917],
    addend=@Long[11],
    mask=@Long[281474976710655],
    DOUBLE_UNIT=@Double[1.1102230246251565E-16],
    BadBound=@String[bound must be positive],
    BadRange=@String[bound must be greater than origin],
    BadSize=@String[size must be non-negative],
    seedUniquifier=@AtomicLong[-3282039941672302964],
    nextNextGaussian=@Double[0.0],
    haveNextNextGaussian=@Boolean[false],
    serialPersistentFields=@ObjectStreamField[][isEmpty=false;size=3],
    unsafe=@Unsafe[sun.misc.Unsafe@2eaa1027],
    seedOffset=@Long[24],
]
```

* Specify classLoader

Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader using `sc -d <ClassName>`.

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.

```bash
$ getstatic -c 3d4eac69 demo.MathGame random
```

For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`getstatic --classLoaderClass demo.MathGame random`

  * PS: Here the classLoaderClass in java 8 is sun.misc.Launcher$AppClassLoader, while in java 11 it's jdk.internal.loader.ClassLoaders$AppClassLoader. Currently katacoda using java 8.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

Tip: if the static field is a complex class, you can even use [`OGNL`](https://commons.apache.org/proper/commons-ognl/language-guide.html) to traverse, filter and access the inner properties of this class.

* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)
* [Special usages](https://github.com/alibaba/arthas/issues/71)


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
