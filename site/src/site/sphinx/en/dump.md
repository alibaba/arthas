dump
===

> Dump the bytecode for the particular classes to the specified directory.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|class name pattern|
|`[c:]`|hashcode of the [class loader](classloader.md) that loaded the target class|
|`[E]`|turn on regex match, the default behavior is wild card match|

### Usage

```bash
$ dump java.lang.String
 HASHCODE  CLASSLOADER  LOCATION
 null                   /Users/admin/logs/arthas/classdump/java/lang/String.class
Affect(row-cnt:1) cost in 119 ms.
```

```bash
$ dump demo.*
 HASHCODE  CLASSLOADER                                    LOCATION
 3d4eac69  +-sun.misc.Launcher$AppClassLoader@3d4eac69    /Users/admin/logs/arthas/classdump/sun.misc.Launcher$AppClassLoader-3d4eac69/demo/MathGame.class
             +-sun.misc.Launcher$ExtClassLoader@66350f69
Affect(row-cnt:1) cost in 39 ms.
```