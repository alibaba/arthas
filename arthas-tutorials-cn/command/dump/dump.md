> dump 已加载类的 bytecode 到特定目录

[dump 命令文档](https://arthas.aliyun.com/doc/dump.html)

### 使用参考

不指定目录加载类的 bytecode
`dump java.lang.String`{{execute T2}}

`dump demo.*`{{execute T2}}

加载类的 bytecode 到 `/tmp/output`
`dump -d /tmp/output java.lang.String`{{execute T2}}

- 指定 classLoader  
  注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。  
  如果你使用`-c`，你需要手动输入 hashcode：`-c <hashcode>`  
  对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：

`dump --classLoaderClass sun.misc.Launcher$AppClassLoader demo.*`{{execute T2}}

- 注：这里 classLoaderClass 在 java 8 是 sun.misc.Launcher$AppClassLoader，而 java 11 的 classloader 是 jdk.internal.loader.ClassLoaders$AppClassLoader，katacoda 目前环境是 java8。

`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。
