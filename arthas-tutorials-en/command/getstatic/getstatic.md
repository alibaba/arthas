- It is recommended to use the [OGNL] (https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=ognl) command, which will be more flexible.

Use [getstatic command](https://arthas.aliyun.com/en/doc/getstatic.html) to Check the static fields of classes conveniently, the usage is `getstatic class_name field_name`.

`getstatic demo.MathGame random`{{execute T2}}

- Specify classLoader
  Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader using `sc -d <ClassName>`.  
  if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.  
  For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`getstatic --classLoaderClass demo.MathGame random`{{execute T2}}

- PS: Here the classLoaderClass in java 8 is sun.misc.Launcher$AppClassLoader, while in java 11 it's jdk.internal.loader.ClassLoaders$AppClassLoader. Currently katacoda using java 8.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

Tip: if the static field is a complex class, you can even use [`OGNL`](https://commons.apache.org/proper/commons-ognl/language-guide.html) to traverse, filter and access the inner properties of this class.

- [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)
- [Special usages](https://github.com/alibaba/arthas/issues/71)
