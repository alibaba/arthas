# classloader

[`classloader`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-classloader)

::: tip
查看 classloader 的继承树，urls，类加载信息
:::

`classloader` 命令将 JVM 中所有的 classloader 的信息统计出来，并可以展示继承树，urls 等。

可以让指定的 classloader 去 getResources，打印出所有查找到的 resources 的 url。对于`ResourceNotFoundException`比较有用。

## 参数说明

|              参数名称 | 参数说明                                   |
| --------------------: | :----------------------------------------- |
|                   [l] | 按类加载实例进行统计                       |
|                   [t] | 打印所有 ClassLoader 的继承树              |
|                   [a] | 列出所有 ClassLoader 加载的类，请谨慎使用  |
|                `[c:]` | ClassLoader 的 hashcode                    |
| `[classLoaderClass:]` | 指定执行表达式的 ClassLoader 的 class name |
|             `[c: r:]` | 用 ClassLoader 去查找 resource             |
|          `[c: load:]` | 用 ClassLoader 去加载指定的类              |

## 使用参考

### 按类加载类型查看统计信息

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

### 按类加载实例查看统计信息

```bash
$ classloader -l
 name                                                loadedCount  hash      parent
 BootstrapClassLoader                                1861         null      null
 com.taobao.arthas.agent.ArthasClassloader@68b31f0a  2115         68b31f0a  sun.misc.Launcher$ExtClassLoader@66350f69
 sun.misc.Launcher$AppClassLoader@3d4eac69           4            3d4eac69  sun.misc.Launcher$ExtClassLoader@66350f69
 sun.misc.Launcher$ExtClassLoader@66350f69           1            66350f69  null
Affect(row-cnt:4) cost in 2 ms.
```

### 查看 ClassLoader 的继承树

```bash
$ classloader -t
+-BootstrapClassLoader
+-sun.misc.Launcher$ExtClassLoader@66350f69
  +-com.taobao.arthas.agent.ArthasClassloader@68b31f0a
  +-sun.misc.Launcher$AppClassLoader@3d4eac69
Affect(row-cnt:4) cost in 3 ms.
```

### 查看 URLClassLoader 实际的 urls

```bash
$ classloader -c 3d4eac69
file:/private/tmp/math-game.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect(row-cnt:9) cost in 3 ms.
```

_注意_ hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。

对于只有唯一实例的 ClassLoader 可以通过 class name 指定，使用起来更加方便：

```bash
$ classloader --classLoaderClass sun.misc.Launcher$AppClassLoader
file:/private/tmp/math-game.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect(row-cnt:9) cost in 3 ms.
```

### 使用 ClassLoader 去查找 resource

```bash
$ classloader -c 3d4eac69  -r META-INF/MANIFEST.MF
 jar:file:/System/Library/Java/Extensions/MRJToolkit.jar!/META-INF/MANIFEST.MF
 jar:file:/private/tmp/math-game.jar!/META-INF/MANIFEST.MF
 jar:file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar!/META-INF/MANIFEST.MF
```

也可以尝试查找类的 class 文件：

```bash
$ classloader -c 1b6d3586 -r java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar!/java/lang/String.class
```

### 使用 ClassLoader 去加载类

```bash
$ classloader -c 3d4eac69 --load demo.MathGame
load class success.
 class-info        demo.MathGame
 code-source       /private/tmp/math-game.jar
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

### 统计 ClassLoader 实际使用 URL 和未使用的 URL

::: warning
注意，基于 JVM 目前已加载的所有类统计，不代表`Unused URLs`可以从应用中删掉。因为可能将来需要从`Unused URLs`里加载类，或者需要加载`resources`。
:::

```
$ classloader --url-stat
 com.taobao.arthas.agent.ArthasClassloader@3c41660, hash:3c41660
 Used URLs:
 file:/Users/admin/.arthas/lib/3.5.6/arthas/arthas-core.jar
 Unused URLs:

 sun.misc.Launcher$AppClassLoader@75b84c92, hash:75b84c92
 Used URLs:
 file:/Users/admin/code/java/arthas/math-game/target/math-game.jar
 file:/Users/admin/.arthas/lib/3.5.6/arthas/arthas-agent.jar
 Unused URLs:

 sun.misc.Launcher$ExtClassLoader@7f31245a, hash:7f31245a
 Used URLs:
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/sunec.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/sunjce_provider.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/localedata.jar
 Unused URLs:
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/nashorn.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/cldrdata.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/legacy8ujsse.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/jfxrt.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/dnsns.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/openjsse.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/sunpkcs11.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/jaccess.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/zipfs.jar
```
