ognl
===

[`ognl` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-ognl)

> Execute ognl expression.

Since 3.0.5.

### Parameters

|Name|Specification|
|---:|:---|
|*express*|expression to be executed|
|`[c:]`| The hashcode of the ClassLoader that executes the expression, default ClassLoader is SystemClassLoader. |
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |
|[x]|Expand level of object (1 by default).|


### Usage

* [Special usages](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

Call static method:

```bash
$ ognl '@java.lang.System@out.println("hello")'
null
```

Get static field:

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


Specify ClassLoader by hashcode: 

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
Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.

For ClassLoader with only unique instance, it can be specified by class name, which is more convenient to use:

```bash
$ ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader  @org.springframework.boot.SpringApplication@logger
@Slf4jLocationAwareLog[
    FQCN=@String[org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog],
    name=@String[org.springframework.boot.SpringApplication],
    logger=@Logger[Logger[org.springframework.boot.SpringApplication]],
]
```

Execute a multi-line expression, and return a list:

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/opt/java/8.0.181-zulu/jre],
    @String[OpenJDK Runtime Environment],
]
```