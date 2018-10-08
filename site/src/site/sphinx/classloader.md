classloader
===

> 查看classloader的继承树，urls，类加载信息

`classloader` 命令将 JVM 中所有的classloader的信息统计出来，并可以展示继承树，urls等。

可以让指定的classloader去getResources，打印出所有查找到的resources的url。对于`ResourceNotFoundException`比较有用。


### 参数说明

|参数名称|参数说明|
|---:|:---|
|[l]|按类加载实例进行统计|
|[t]|打印所有ClassLoader的继承树|
|[a]|列出所有ClassLoader加载的类，请谨慎使用|
|`[c:]`|ClassLoader的hashcode|
|`[c: r:]`|用ClassLoader去查找resource|

### 使用参考

* 按类加载类型查看统计信息

```s
$ classloader
 name                                                   numberOfInstances  loadedCountTotal
 com.taobao.pandora.service.loader.ModuleClassLoader    29                 11659
 com.taobao.pandora.boot.loader.ReLaunchURLClassLoader  1                  5308
 BootstrapClassLoader                                   1                  3711
 com.taobao.arthas.agent.ArthasClassloader              2                  2825
 sun.reflect.DelegatingClassLoader                      332                332
 java.net.URLClassLoader                                1                  285
 sun.misc.Launcher$AppClassLoader                       1                  77
 sun.misc.Launcher$ExtClassLoader                       1                  46
 com.alibaba.fastjson.util.ASMClassLoader               2                  3
 org.jvnet.hk2.internal.DelegatingClassLoader           2                  2
 sun.reflect.misc.MethodUtil                            1                  1
Affect(row-cnt:11) cost in 66 ms.
```

* 按类加载实例查看统计信息

```bash
$ classloader -l
 name                                                            loadedCount  hash      parent
 BootstrapClassLoader                                            3711         null      null
 com.alibaba.fastjson.util.ASMClassLoader@3bbaa1b8               2            3bbaa1b8  monitor's ModuleClassLoader
 com.alibaba.fastjson.util.ASMClassLoader@5e255d0b               1            5e255d0b  eagleeye-core's ModuleClassLoader
 com.taobao.arthas.agent.ArthasClassloader@4fa2d7e6              1795         4fa2d7e6  sun.misc.Launcher$ExtClassLoader@a38d7a3
 com.taobao.arthas.agent.ArthasClassloader@522400c2              1033         522400c2  sun.misc.Launcher$ExtClassLoader@a38d7a3
 com.taobao.pandora.boot.loader.ReLaunchURLClassLoader@1817d444  5308         1817d444  sun.misc.Launcher$AppClassLoader@14dad5dc
 tbsession's ModuleClassLoader                                   285          609cd4d8  null
 pandora-qos-service's ModuleClassLoader                         267          2f8dad04  null
 pandora-framework's ModuleClassLoader                           78           4009e306  null
 filesync-client's ModuleClassLoader                             4            4b8ee4de  null
 rocketmq-client's ModuleClassLoader                             431          247bddad  null
 eagleeye-core's ModuleClassLoader                               451          1ba9117e  null
 alimonitor-jmonitor's ModuleClassLoader                         134          22fcf7ab  null
 metaq-client's ModuleClassLoader                                35           41a2befb  null
 hsf-mock's ModuleClassLoader                                    3            2002fc1d  null
 monitor's ModuleClassLoader                                     1427         131ef10   null
 spas-sdk-service's ModuleClassLoader                            7            10d307f1  null
 vipserver-client's ModuleClassLoader                            137          7a419da4  null
 metrics's ModuleClassLoader                                     146          696da30b  null
 mtop-uncenter's ModuleClassLoader                               922          79d8407f  null
 spas-sdk-client's ModuleClassLoader                             235          4944252c  null
 live-profiler-pandora's ModuleClassLoader                       1            6913c1fb  null
 notify-tr-client's ModuleClassLoader                            472          fba92d3   null
 ons-sdk's ModuleClassLoader                                     70           23348b5d  null
 tair-plugin's ModuleClassLoader                                 1053         7c9d8e2   null
 tddl-client's ModuleClassLoader                                 2354         4988d8b8  null
 config-client's ModuleClassLoader                               93           429bffaa  null
 diamond-client's ModuleClassLoader                              360          3d5c822d  null
 pandolet's ModuleClassLoader                                    99           41e1e210  null
 hsf's ModuleClassLoader                                         1796         3232a28a  null
 acl.plugin's ModuleClassLoader                                  379          67080771  null
 buc.sso.client.plugin's ModuleClassLoader                       195          13b6aecc  null
 unitrouter's ModuleClassLoader                                  64           7e5afaa6  null
 switch's ModuleClassLoader                                      104          24313fcc  null
 hsf-notify-client's ModuleClassLoader                           57           4d0f2471  null
 java.net.URLClassLoader@7ec7ffd3                                285          7ec7ffd3  sun.misc.Launcher$ExtClassLoader@a38d7a3
 javax.management.remote.rmi.NoCallStackClassLoader@53f65459     1            53f65459  null
 javax.management.remote.rmi.NoCallStackClassLoader@2833cc44     1            2833cc44  null
 org.jvnet.hk2.internal.DelegatingClassLoader@72cda8ee           1            72cda8ee  monitor's ModuleClassLoader
 org.jvnet.hk2.internal.DelegatingClassLoader@1f57f96d           1            1f57f96d  monitor's ModuleClassLoader
 sun.misc.Launcher$AppClassLoader@14dad5dc                       77           14dad5dc  sun.misc.Launcher$ExtClassLoader@a38d7a3
 sun.misc.Launcher$ExtClassLoader@a38d7a3                        46           a38d7a3   null
 sun.reflect.misc.MethodUtil@1201f221                            1            1201f221  sun.misc.Launcher$AppClassLoader@14dad5dc
```

* 查看ClassLoader的继承树

```shell
$ classloader -t
+-BootstrapClassLoader
+-unitrouter's ModuleClassLoader
+-diamond-client's ModuleClassLoader
+-sun.misc.Launcher$ExtClassLoader@548a102f
| +-sun.misc.Launcher$AppClassLoader@14dad5dc
|   +-com.taobao.arthas.agent.AgentLauncher$1@334e6bb8
|   | +-sun.reflect.DelegatingClassLoader@328b3a05
|   | +-sun.reflect.DelegatingClassLoader@73f44f24
```

* 查看URLClassLoader实际的urls

```shell
$ classloader -c 5ffe9775
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/commons-lang-2.6.jar
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/log4j-1.2.16.jar
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/logger.api-0.1.4.jar
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/pandora.api-2.0.7-SNAPSHOT.jar
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/pandora.container-2.0.7-SNAPSHOT.jar
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/pandora.thirdcontainer-2.0.7-SNAPSHOT.jar
file:/Users/hello/soft/taobao-tomcat-7.0.64/deploy/taobao-hsf.sar/lib/picocontainer-2.14.3.jar
```

* 使用ClassLoader去查找resource

```shell
$ classloader -c 226b143b -r META-INF/MANIFEST.MF
 jar:file:/Users/hello/.m2/repository/javax/enterprise/cdi-api/1.0/cdi-api-1.0.jar!/META-INF/MANIFEST.MF
 jar:file:/Users/hello/.m2/repository/javax/annotation/jsr250-api/1.0/jsr250-api-1.0.jar!/META-INF/MANIFEST.MF
```

也可以尝试查找类的class文件：

```shell
$ classloader -c 1b6d3586 -r java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar!/java/lang/String.class
```
