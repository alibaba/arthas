dump
===

Dump bytecode of the loaded classes to a specified directory.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|[c:]|hashcode of the [class loader](classloader.md) that loaded the class|
|[E]|turn on regex matching while the default is wildcard matching|

### Usage

```bash
$ dump demo.Demo$Counter
 HASHCODE  CLASSLOADER                                    LOCATION                                                                                                                   
 659e0bfd  +-sun.misc.Launcher$AppClassLoader@659e0bfd    /Users/lhearen/logs/arthas/classdump/sun.misc.Launcher$AppClassLoader-659e0bfd/demo/Demo$Counter.class                     
             +-sun.misc.Launcher$ExtClassLoader@758c1b43                                                                                                                             
Affect(row-cnt:1) cost in 25 ms.
```
