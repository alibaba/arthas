Arthas 3.0新特性介绍
===

Arthas 3.0在架构上做了重大改造，通过引入`termd`完整的支持了telnet协议，并基于websocket封装了telnet协议实现了`Telnet over HTTP`，使得在线诊断成为了可能，并且保证了本地启动，telnet远程访问，在线诊断三种方式的体验完全一致。都支持自动补全，高亮显示等功能。并且支持`多人同时在线诊断`。

### 在线诊断

Arthas 3.0最重要的特性，通过Arthas在线诊断平台，无需再登陆目标机器，一键启动Arthas并开启诊断！

![image](TODO image.png)


具体使用方法请参见[在线诊断使用说明](https://github.com/alibaba/arthas/wiki/arthas_3_0/home)

### 管道支持

Arthas 3.0开始支持管道, 率先提供了`grep`,`wc`,`plaintext`的支持。

```bash
 java.vendor.url                                      http://java.oracle.com/
 java.vm.vendor                                       Oracle Corporation
 java.runtime.name                                    Java(TM) SE Runtime Environment
 sun.java.command                                     org.apache.catalina.startup.Bootstrap start
 java.class.path                                      /Users/wangtao/work/ali-tomcat-home/ant-develop/output/build/bin/bootstrap.jar
 java.vm.specification.name                           Java Virtual Machine Specification
 java.vm.specification.version                        1.8
 java.awt.headless                                    true
 java.io.tmpdir                                       /Users/wangtao/work/ali-tomcat-home/ant-develop/output/build/temp
 java.vendor.url.bug                                  http://bugreport.sun.com/bugreport/
 java.awt.graphicsenv                                 sun.awt.CGraphicsEnvironment
 java.ext.dirs                                        /Users/wangtao/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.
                                                      /Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java
 java.vm.name                                         Java HotSpot(TM) 64-Bit Server VM
 java.specification.version                           1.8
$ sysprop | grep java | wc -l
36
```


### 启动自检

针对启动时经常出现的权限问题，在启动脚本中增加了自检逻辑，脚本会自动判断目标进程是否具备attach权限，并给出进一步的提示。

```
[huxing.zhx@v125056161.bja /home/huxing.zhx]
$./as.sh 32260
The current user (huxing.zhx) does not match with the owner of process 32260 (admin).
To solve this, choose one of the following command:
  1) sudo su admin && ./as.sh
  2) sudo -u admin -EH ./as.sh
```


### 去groovy依赖

groovy表达式在arthas2.0中大量使用，例如watch表达式

```bash
watch com.alibaba.sample.petstore.web.store.module.screen.ItemList add "params + ' ' + returnObj" params.size()==2
```

其中`"params + ' ' + returnObj"`以及`params.size()==2`背后其实都使用了groovy来进行表达式求值，如果反复大量的运行这些表达式，groovy会创建大量的classloader，打满perm区从而触发FGC。

为了避免这个问题，Arthas 3.0中使用了ognl这个更加轻量的表达式求值库来代替groovy，彻底解决了groovy引起的FGC风险。但由于这个替换，导致原来使用groovy脚本编写的自定义脚本失效。这个问题留待后续解决。

在3.0中，watch命令的表达式部分的书写有了一些改变，详见[这里](https://arthas.aliyun.com/doc/watch)
### 提升rt统计精度

Arthas 2.0中，统计rt都是以`ms`为单位，对于某些比较小的方法调用，耗时在毫秒以下的都会被认为是0ms，造成trace总时间和各方法的时间相加不一致等问题（虽然这里面确实会有误差，主要Arthas自身的开销）。Arthas 3.0中所有rt的单位统一改为使用`ns`来统计，精准捕获你的方法耗时，让0ms这样无意义的统计数据不再出现！

```
$ tt -l
 INDEX     TIMESTAMP               COST(ms)    IS-RET    IS-EXP   OBJECT            CLASS                                METHOD
------------------------------------------------------------------------------------------------------------------------------------------------------------
 1000      2017-02-24 10:56:46     808.743525  true      false    0x3bd5e918        TestTraceServlet                     doGet
 1001      2017-02-24 10:56:55     805.799155  true      false    0x3bd5e918        TestTraceServlet                     doGet
 1002      2017-02-24 10:57:04     808.026935  true      false    0x3bd5e918        TestTraceServlet                     doGet
 1003      2017-02-24 10:57:22     805.036963  true      false    0x3bd5e918        TestTraceServlet                     doGet
 1004      2017-02-24 10:57:24     803.581886  true      false    0x3bd5e918        TestTraceServlet                     doGet
 1005      2017-02-24 10:57:39     814.657657  true      false    0x3bd5e918        TestTraceServlet                     doGet
```

### watch/stack/trace命令支持按耗时过滤

我们在trace的时候，经常会出现某个方法间隙性的rt飙高，但是我们只想知道rt高的时候，是哪里慢了，对于正常rt的方法我们并不关心，Arthas 3.0支持了按`#cost`(方法执行耗时,单位为`ms`)进行过滤，只输出符合条件的trace路径。

具体用法为：

```
trace/watch/stack class_name method cost_expression
```

例如：

```
$ trace  test.arthas.TestTraceServlet doGet #cost>800
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 137 ms.
trace  test.arthas.TestTraceServlet doGet #cost>800
`---thread_name=http-bio-8080-exec-9;id=6e;is_daemon=true;priority=5;TCCL=org.apache.catalina.loader.WebappClassLoader
    `---[816.880001ms] test.arthas.TestTraceServlet:doGet()
        +---[min=0.019223ms,max=1.192115ms,total=1.211338ms,count=2] java.lang.System:currentTimeMillis()
        +---[1.407006ms] test.arthas.TestTraceServlet:call1()
        +---[381.970892ms] test.arthas.TestTraceServlet:callhsfL1()
        +---[0.018866ms] test.arthas.TestPathTrace:<init>()
        +---[375.753301ms] test.arthas.TestPathTrace:callPathTrace()
        +---[12.352252ms] test.arthas.TestTraceServlet:call3()
        +---[2.758025ms] test.arthas.TestTraceServlet:call4()
        +---[1.246057ms] test.arthas.TestTraceServlet:call5()
        +---[10.306568ms] test.arthas.TestTraceServlet:call6()
        +---[8.891933ms] test.arthas.TestTraceServlet:call7()
        +---[4.030325ms] test.arthas.TestTraceServlet:call8()
        +---[6.51316ms] test.arthas.TestTraceServlet:call9()
        +---[0.059405ms] javax.servlet.http.HttpServletResponse:getWriter()
        +---[0.013107ms] java.lang.StringBuilder:<init>()
        +---[min=0.004892ms,max=0.06357ms,total=0.100672ms,count=3] java.lang.StringBuilder:append()
        +---[0.018255ms] java.lang.StringBuilder:toString()
        `---[0.028812ms] java.io.PrintWriter:write()
```

上述命令只有当`test.arthas.TestTraceServlet#doGet`方法执行耗时大于800ms时才会输出。

### trace命令优化

#### 自动高亮显示最耗时方法调用

trace命令现在会自动显示

![image](TODO/image.png)


#### 带条件过滤的多级trace

目前trace默认只输出一级方法调用耗时，有时候并不能完全看出问题。但是如果展开多级的话，每一个方法的耗时都统计，会造成方法数量迅速膨胀，大大增加trace的开销。

TODO

#### 显示当前线程的信息及eagleeye的traceId

```
trace  test.arthas.TestTraceServlet doGet
`---thread_name=http-bio-8080-exec-10;id=da;is_daemon=true;priority=5;TCCL=org.apache.catalina.loader.WebappClassLoader;trace_id=1e09489014879085429791006d969d
```


### sysprop命令操作SystemProperty

sysprop命令支持查看所有的系统属性，以及针对特定属性进行查看和修改。

```
$ sysprop
...
 os.arch                                              x86_64
 java.ext.dirs                                        /Users/wangtao/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.
                                                      8.0_51.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library
                                                      /Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java
 user.dir                                             /Users/wangtao/work/ali-tomcat-home/ant-develop/output/build
 catalina.vendor                                      alibaba
 line.separator

 java.vm.name                                         Java HotSpot(TM) 64-Bit Server VM
 file.encoding                                        UTF-8
 org.apache.tomcat.util.http.ServerCookie.ALLOW_EQUA  true
 LS_IN_VALUE
 com.taobao.tomcat.info                               Apache Tomcat/7.0.70.1548
 java.specification.version                           1.8
$ sysprop java.version
java.version=1.8.0_51
$ sysprop production.mode true
Successfully changed the system property.
production.mode=true
```

### thread命令支持指定采样时间

thread命令计算线程cpu占用的逻辑，默认是采样100ms内各个线程的cpu使用情况并计算cpu消耗占比。有时候100ms的时间间隔太短，看不出问题所在，Arthas3.0中thread命令支持设置采样间隔(以`ms`为单位)，可以观察任意时间段内的cpu消耗占比情况。

```
$ thread -i 1000                                                                                                                                                                                                                     
Threads Total: 74, NEW: 0, RUNNABLE: 17, BLOCKED: 0, WAITING: 15, TIMED_WAITING: 42, TERMINATED: 0                                                                                                                                   
ID                 NAME                                                     GROUP                                  PRIORITY           STATE              %CPU               TIME               INTERRUPTED        DAEMON             
78                 com.taobao.config.client.timer                           main                                   5                  TIMED_WAITING      22                 0:0                false              true               
92                 Abandoned connection cleanup thread                      main                                   5                  TIMED_WAITING      15                 0:2                false              true               
361                as-command-execute-daemon                                system                                 10                 RUNNABLE           14                 0:0                false              true               
67                 HSF-Remoting-Timer-10-thread-1                           main                                   10                 TIMED_WAITING      12                 0:2                false              true               
113                JamScheduleThread                                        system                                 9                  TIMED_WAITING      2                  0:0                false              true               
14                 Thread-3                                                 main                                   5                  RUNNABLE           2                  0:0                false              false              
81                 com.taobao.remoting.TimerThread                          main                                   5                  TIMED_WAITING      2                  0:0                false              true               
104                http-bio-7001-AsyncTimeout                               main                                   5                  TIMED_WAITING      2                  0:0                false              true               
123                nioEventLoopGroup-2-1                                    system                                 10                 RUNNABLE           2                  0:0                false              false              
127                nioEventLoopGroup-3-2                                    system                                 10                 RUNNABLE           2                  0:0                false              false              
345                nioEventLoopGroup-3-3                                    system                                 10                 RUNNABLE           2                  0:0                false              false              
358                nioEventLoopGroup-3-4                                    system                                 10                 RUNNABLE           2                  0:0                false              false              
27                 qos-boss-1-1                                             main                                   5                  RUNNABLE           2                  0:0                false              true               
22                 EagleEye-AsyncAppender-Thread-BizLog                     main                                   5                  TIMED_WAITING      1                  0:0                false              true               
```