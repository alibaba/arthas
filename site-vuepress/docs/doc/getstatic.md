getstatic
===

[`getstatic`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-getstatic)

* 推荐直接使用[ognl](ognl.md)命令，更加灵活。

通过getstatic命令可以方便的查看类的静态属性。使用方法为`getstatic class_name field_name`


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

* 指定classLoader

注意hashcode是变化的，需要先查看当前的ClassLoader信息，使用`sc -d <ClassName>`提取对应ClassLoader的hashcode。

如果你使用`-c`，你需要手动输入hashcode：`-c <hashcode>`

```bash
$ getstatic -c 3d4eac69 demo.MathGame random
```

对于只有唯一实例的ClassLoader可以通过`--classLoaderClass`指定class name，使用起来更加方便：

`getstatic --classLoaderClass sun.misc.Launcher$AppClassLoader demo.MathGame random`

  * 注: 这里classLoaderClass 在 java 8 是 sun.misc.Launcher$AppClassLoader，而java 11的classloader是jdk.internal.loader.ClassLoaders$AppClassLoader，katacoda目前环境是java8。

`--classLoaderClass` 的值是ClassLoader的类名，只有匹配到唯一的ClassLoader实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。


如果该静态属性是一个复杂对象，还可以支持在该属性上通过ognl表示进行遍历，过滤，访问对象的内部属性等操作。

* OGNL特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
* OGNL表达式官方指南：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)


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