> Dump the bytecode for the particular classes to the specified directory.

[dump command Docs](https://arthas.aliyun.com/en/doc/dump.html)

### Usage

Loading class bytecode without specifying a directory
`dump java.lang.String`{{execute T2}}

`dump demo.*`{{execute T2}}

Loading class bytecode with specifying a directory
`dump -d /tmp/output java.lang.String`{{execute T2}}

- Specify classLoader
  Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.  
  if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.  
  For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`dump --classLoaderClass sun.misc.Launcher$AppClassLoader demo.*`{{execute T2}}

- PS: Here the classLoaderClass in java 8 is sun.misc.Launcher$AppClassLoader, while in java 11 it's jdk.internal.loader.ClassLoaders$AppClassLoader. Currently katacoda using java 8.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.
