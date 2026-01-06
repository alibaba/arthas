# ognl

[`ognl`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-ognl)

::: tip
执行 ognl 表达式
:::

从 3.0.5 版本增加

## 参数说明

|              参数名称 | 参数说明                                                         |
| --------------------: | :--------------------------------------------------------------- |
|             _express_ | 执行的表达式                                                     |
|                `[c:]` | 执行表达式的 ClassLoader 的 hashcode，默认值是 SystemClassLoader |
| `[classLoaderClass:]` | 指定执行表达式的 ClassLoader 的 class name                       |
|                   [x] | 结果对象的展开层次，默认值 1                                     |

## 使用参考

- OGNL 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
- OGNL 表达式官方指南：[https://commons.apache.org/dormant/commons-ognl/language-guide.html](https://commons.apache.org/dormant/commons-ognl/language-guide.html)

## 对象引用存储器（#ref）

Arthas 在 OGNL 上下文里内置了 `#ref` 变量，用于在多次命令之间共享对象引用。

`#ref` 保存的是**弱引用**，不会阻止应用 JVM 回收对象，所以 `get()` 可能返回 `null`（对象已被 GC），这是正常行为。

`#ref` 是全局共享的（同一个 Arthas 进程内所有连接可见）。建议使用命名空间隔离/协作：

- 为避免 key 无限制增长，`#ref` 内部有容量上限，超过后会按 LRU（最近最少使用）策略淘汰。
- 存入：`#ref.ns("case-123").put("name", obj)`
- 取出：`#ref.ns("case-123").get("name")`
- 列表：`#ref.ns("case-123").ls()`
- 删除：`#ref.ns("case-123").remove("name")`
- 清空命名空间：`#ref.ns("case-123").clear()`

示例：配合 `watch` 暂存返回值，后续 `ognl` 再取出：

```bash
$ watch demo.MathGame primeFactors '{#ref.ns("case-123").put("ret", returnObj), returnObj}' -x 2 -n 1
$ ognl '#ref.ns("case-123").get("ret")'
```

调用静态函数：

```bash
$ ognl '@java.lang.System@out.println("hello")'
null
```

获取静态类的静态字段：

```bash
$ ognl '@demo.MathGame@random'
@Random[
    serialVersionUID=@Long[3905348978240129619],
    seed=@AtomicLong[125451474443703],
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
    unsafe=@Unsafe[sun.misc.Unsafe@28ea5898],
    seedOffset=@Long[24],
]
```

通过 hashcode 指定 ClassLoader：

```bash
$ classloader -t
+-BootstrapClassLoader
+-jdk.internal.loader.ClassLoaders$PlatformClassLoader@301ec38b
  +-com.taobao.arthas.agent.ArthasClassloader@472067c7
  +-jdk.internal.loader.ClassLoaders$AppClassLoader@4b85612c
    +-org.springframework.boot.loader.LaunchedURLClassLoader@7f9a81e8

$ ognl -c 7f9a81e8 @org.springframework.boot.SpringApplication@logger
@Slf4jLocationAwareLog[
    FQCN=@String[org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog],
    name=@String[org.springframework.boot.SpringApplication],
    logger=@Logger[Logger[org.springframework.boot.SpringApplication]],
]
$
```

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。

对于只有唯一实例的 ClassLoader 可以通过 class name 指定，使用起来更加方便：

```bash
$ ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader  @org.springframework.boot.SpringApplication@logger
@Slf4jLocationAwareLog[
    FQCN=@String[org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog],
    name=@String[org.springframework.boot.SpringApplication],
    logger=@Logger[Logger[org.springframework.boot.SpringApplication]],
]
```

执行多行表达式，赋值给临时变量，返回一个 List：

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/opt/java/8.0.181-zulu/jre],
    @String[OpenJDK Runtime Environment],
]
```
