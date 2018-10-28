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
$ dump org.apache.commons.lang.StringUtils
 HASHCODE  CLASSLOADER                                                        LOCATION
 29505d69  +-tddl-client's ModuleClassLoader                                  /Users/zhuyong/middleware/taobao-tomcat/output/build/bin/classdump/com.taobao.pandora
                                                                              .service.loader.ModuleClassLoader-29505d69/org.apache.commons.lang.StringUtils.class
 6e51ad67  +-java.net.URLClassLoader@6e51ad67                                 /Users/zhuyong/middleware/taobao-tomcat/output/build/bin/classdump/java.net.URLClassL
             +-sun.misc.Launcher$AppClassLoader@6951a712                      oader-6e51ad67/org.apache.commons.lang.StringUtils.class
               +-sun.misc.Launcher$ExtClassLoader@6fafc4c2
 2bdd9114  +-pandora-qos-service's ModuleClassLoader                          /Users/zhuyong/middleware/taobao-tomcat/output/build/bin/classdump/com.taobao.pandora
                                                                              .service.loader.ModuleClassLoader-2bdd9114/org.apache.commons.lang.StringUtils.class
 544dc9ba  +-com.taobao.tomcat.container.context.loader.AliWebappClassLoader  /Users/zhuyong/middleware/taobao-tomcat/output/build/bin/classdump/com.taobao.tomcat.
             +-org.apache.catalina.loader.StandardClassLoader@2302e984        container.context.loader.AliWebappClassLoader-544dc9ba/org.apache.commons.lang.String
               +-sun.misc.Launcher$AppClassLoader@6951a712                    Utils.class
                 +-sun.misc.Launcher$ExtClassLoader@6fafc4c2
 22880c2b  +-java.net.URLClassLoader@22880c2b                                 /Users/zhuyong/middleware/taobao-tomcat/output/build/bin/classdump/java.net.URLClassL
             +-sun.misc.Launcher$AppClassLoader@6951a712                      oader-22880c2b/org.apache.commons.lang.StringUtils.class
               +-sun.misc.Launcher$ExtClassLoader@6fafc4c2
Affect(row-cnt:5) cost in 156 ms.
$ dump -E org\.apache\.commons\.lang\.StringUtils -c 22880c2b
 HASHCODE  CLASSLOADER                                      LOCATION
 22880c2b  +-java.net.URLClassLoader@22880c2b               /Users/zhuyong/middleware/taobao-tomcat/output/build/bin/classdump/java.net.URLClassLoader-22880c2b/org
             +-sun.misc.Launcher$AppClassLoader@6951a712    .apache.commons.lang.StringUtils.class
               +-sun.misc.Launcher$ExtClassLoader@6fafc4c2
Affect(row-cnt:1) cost in 67 ms.
```
