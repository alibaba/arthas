dump
===

> dump 已加载类的 bytecode 到特定目录

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|`[c:]`|类所属 ClassLoader 的 hashcode|
|[E]|开启正则表达式匹配，默认为通配符匹配|

### 使用参考

```shell
$ dump -E org\.apache\.commons\.lang\.StringUtils
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