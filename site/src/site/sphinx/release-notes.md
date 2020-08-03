
Release Notes
===


v3.1.1
---

* [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.1.1](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.1.1)

v3.1.0
---

* [https://github.com/alibaba/arthas/releases/tag/3.1.0](https://github.com/alibaba/arthas/releases/tag/3.1.0)


v3.0.5
---

* [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.5](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.5)

v3.0.4
---

* [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.4](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.4)


v2017-11-03
----

* 增加getstatic方法获取静态变量
* 修复arthas classloader加载到应用日志的问题
* 增加ognl custom classloader便于调用静态方法
* 优化termd输出大字符串的性能问题
* classloader命令默认按类加载器类型分类编译
* 修复wc命令统计错误的问题
* 禁止增强特定JDK类，如Classloader, Method, Integer等
* 支持OGNL表达式出错直接退出命令
* 修复管道类命令单独出错的问题
* 优化命令重定向功能，使用异步日志输出结果
* trace命令增加过滤jdk方法调用的功能


v2017-09-22
----

* 优化agent server启动时的异常信息
* 修复异步命令的一些bug

v2017-09-11
----

* 支持[异步后后命令](async.md)
* jad命令优化，支持JDK8及内部类
* 修复中文乱码问题

v2017-05-11
----

* tt命令默认只展开1层，防止对象过大造成卡顿
* 修复中文无法展示的问题

v2017-05-12
----

* Arthas 3.0 release

v2016-12-09
----

* as.sh支持-h输出帮助
* [#121] 修复残留的临时文件导致arthas启动失败的问题
* [#123] 修复反复attach/shutdown造成classloader泄露的问题
* 优化命令中的帮助提示信息
* [#126] 修复tm命令文档链接错乱的问题
* [#122] classloader命令中过滤掉`sun.reflect.DelegatingClassLoader`
* [#129] 修复classloader层次展示的问题
* [#125] arthas输出的log不主动换行，对于日志解析更加友好
* [#96] sc等命令支持com/taobao/xxx/TestClass这样的格式，以后复制粘贴不需要在把'/'替换成'.'啦
* [#124] 修复某些情况下trace的时间为负值的问题
* [#128] tt命令的结果默认自动展开，不需要再增加`-x 2`来看到参数，异常的详细信息了。
* [#130] 修复当端口冲突时，没有很好地打印错误，而是进入了一个出错的交互界面的问题
* [#98] 修复Arthas启动时，如果下载更新失败，导致启动失败的问题
* [#139] 修复某些特殊情况下agent attach失败的问题
* [#156] jd-core-java延迟初始化，避免arthas启动时出错
* 修复线程名重复的问题
* [#150] trace命令支持按运行总耗时过滤
* 修复sc查找SystemClassloader时可能出现的NPE
* [#180] 修复第一次Attach成功之后，删除之前Arthas的安装包，重新编译打包，再次attach失败的问题


v2016-06-07
----

* 修复以资源方式加载spy类时出现NPE的问题
* 支持一键找出线程中获得锁并阻塞住其他线程的线程
* 优化 Thread 输出，按线程名排序
* 获取topN忙的线程时，支持指定刷新间隔

v2016-04-08
----

* New feature：
    * dashboard支持指定刷新频率，支持指定执行次数
    * 命令执行结果保存到日志文件，方便后续查看
    * 启动速度优化，第一次attach的速度提升一倍
    * 支持批处理功能，支持执行脚本文件
    * 优化启动逻辑，arthas脚本启动时交互式选择进程
    * 类默认启用继承关系查询，查找类时默认会查找子类，如果需要关闭，则通过全局开关 Options disable-sub-class 关闭
    * 支持在彩色模式和文本模式中切换

* UI Improvement:
    * 合并exit和quit命令
    * 命令帮助信息增加wiki链接
    * 优化watch的逻辑，更加符合大家的直觉
    * thread命令增加example说明
    * 自动补全的时候，忽略大小写

* Bugfix:
    * 修复trace命令遇到循环造成输出太长
    * 修复trace命令在方法调用中抛出了异常，会让trace的节点错位
    * 修正增强BootstrapClassLoader加载的类，找不到Spy的问题
    * 修复某些配色方案下，结果显示不友好的问题

v2016-03-07
----

* 支持一键查看当前最忙的前N个线程及其堆栈
* 修复openjdk下启动arthas失败的问题（需要重新安装as.sh）
* 一些体验优化


v2016-01-18
----

* 优化 jad，实时 dump 内存 byte array，并使用 jd-core-java 反编译，支持`行号显示`
* 修复 tt 命令在监控与线程上下文相关的方法调用时，显示/重做等场景下的 bug 

v2016-01-08
----

* 修复一些 bug
    * jad NPE
    * watch/monitor NPE
    * 不需要转义
    * 数据统计问题修复
    * sc 查看静态变量内部层次结构 

v2015-12-29
---

* Arthas 2.0 测试版本发布！