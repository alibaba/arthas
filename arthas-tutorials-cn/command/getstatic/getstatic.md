- 推荐直接使用[ognl](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=ognl)命令，更加灵活。

通过 [getstatic 命令](https://arthas.aliyun.com/doc/getstatic.html) 可以方便的查看类的静态属性。使用方法为`getstatic class_name field_name`

`getstatic demo.MathGame random`{{execute T2}}

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

- 指定 classLoader

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，使用`sc -d <ClassName>`提取对应 ClassLoader 的 hashcode。

如果你使用`-c`，你需要手动输入 hashcode：`-c <hashcode>`

```bash
$ getstatic -c 3d4eac69 demo.MathGame random
```

对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：

`getstatic --classLoaderClass sun.misc.Launcher$AppClassLoader demo.MathGame random`{{execute T2}}

- 注：这里 classLoaderClass 在 java 8 是 sun.misc.Launcher$AppClassLoader，而 java 11 的 classloader 是 jdk.internal.loader.ClassLoaders$AppClassLoader，katacoda 目前环境是 java8。

`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

如果该静态属性是一个复杂对象，还可以支持在该属性上通过 ognl 表示进行遍历，过滤，访问对象的内部属性等操作。

- OGNL 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
- OGNL 表达式官方指南：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)
