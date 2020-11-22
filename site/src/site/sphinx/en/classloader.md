classloader
===========

[`classloader` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-classloader)

View hierarchy, urls and classes-loading info for the class-loaders.

`classloader` can search and print out the URLs for a specified resource from one particular classloader. It is quite handy when analyzing `ResourceNotFoundException`.

### Options

|Name|Specification|
|---:|:---|
|[l]|list all classloader instances|
|[t]|print classloader's hierarchy|
|[a]|list all the classes loaded by all the classloaders (use it with great caution since the output can be huge)|
|[c:]|print classloader's hashcode|
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |
|`[c: r:]`|using ClassLoader to search resource|
|`[c: load:]`|using ClassLoader to load class|

### Usage

#### View statistics categorized by class type

```bash
$ classloader
 name                                       numberOfInstances  loadedCountTotal
 com.taobao.arthas.agent.ArthasClassloader  1                  2115
 BootstrapClassLoader                       1                  1861
 sun.reflect.DelegatingClassLoader          5                  5
 sun.misc.Launcher$AppClassLoader           1                  4
 sun.misc.Launcher$ExtClassLoader           1                  1
Affect(row-cnt:5) cost in 3 ms.
```

#### View statistics categorized by loaded classes number

```bash
$ classloader -l
 name                                                loadedCount  hash      parent
 BootstrapClassLoader                                1861         null      null
 com.taobao.arthas.agent.ArthasClassloader@68b31f0a  2115         68b31f0a  sun.misc.Launcher$ExtClassLoader@66350f69
 sun.misc.Launcher$AppClassLoader@3d4eac69           4            3d4eac69  sun.misc.Launcher$ExtClassLoader@66350f69
 sun.misc.Launcher$ExtClassLoader@66350f69           1            66350f69  null
Affect(row-cnt:4) cost in 2 ms.
```

#### View class-loaders hierarchy

```bash
$ classloader -t
+-BootstrapClassLoader
+-sun.misc.Launcher$ExtClassLoader@66350f69
  +-com.taobao.arthas.agent.ArthasClassloader@68b31f0a
  +-sun.misc.Launcher$AppClassLoader@3d4eac69
Affect(row-cnt:4) cost in 3 ms.
```

#### Show the URLs of the URLClassLoader

```bash
$ classloader -c 3d4eac69
file:/private/tmp/arthas-demo.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect(row-cnt:9) cost in 3 ms.
```

Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.

For ClassLoader with only unique instance, it can be specified by class name, which is more convenient to use:

```bash
$ classloader --classLoaderClass sun.misc.Launcher$AppClassLoader
file:/private/tmp/arthas-demo.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect(row-cnt:9) cost in 3 ms.
```

#### Use the classloader to load resource

```bash
$ classloader -c 3d4eac69  -r META-INF/MANIFEST.MF
 jar:file:/System/Library/Java/Extensions/MRJToolkit.jar!/META-INF/MANIFEST.MF
 jar:file:/private/tmp/arthas-demo.jar!/META-INF/MANIFEST.MF
 jar:file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar!/META-INF/MANIFEST.MF
```

Use the classloader to load `.class` resource

```shell
$ classloader -c 1b6d3586 -r java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar!/java/lang/String.class
```

#### Use the classloader to load class

```bash
$ classloader -c 3d4eac69 --load demo.MathGame
load class success.
 class-info        demo.MathGame
 code-source       /private/tmp/arthas-demo.jar
 name              demo.MathGame
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       MathGame
 modifier          public
 annotation
 interfaces
 super-class       +-java.lang.Object
 class-loader      +-sun.misc.Launcher$AppClassLoader@3d4eac69
                     +-sun.misc.Launcher$ExtClassLoader@66350f69
 classLoaderHash   3d4eac69
```