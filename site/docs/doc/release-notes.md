# Release Notes

## v3.1.1

- [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.1.1](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.1.1)

## v3.1.0

- [https://github.com/alibaba/arthas/releases/tag/3.1.0](https://github.com/alibaba/arthas/releases/tag/3.1.0)

## v3.0.5

- [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.5](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.5)

## v3.0.4

- [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.4](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.4)

## v2017-11-03

- 增加 getstatic 方法获取静态变量
- 修复 arthas classloader 加载到应用日志的问题
- 增加 ognl custom classloader 便于调用静态方法
- 优化 termd 输出大字符串的性能问题
- classloader 命令默认按类加载器类型分类编译
- 修复 wc 命令统计错误的问题
- 禁止增强特定 JDK 类，如 Classloader, Method, Integer 等
- 支持 OGNL 表达式出错直接退出命令
- 修复管道类命令单独出错的问题
- 优化命令重定向功能，使用异步日志输出结果
- trace 命令增加过滤 jdk 方法调用的功能

## v2017-09-22

- 优化 agent server 启动时的异常信息
- 修复异步命令的一些 bug

## v2017-09-11

- 支持[异步后后命令](async.md)
- jad 命令优化，支持 JDK8 及内部类
- 修复中文乱码问题

## v2017-05-11

- tt 命令默认只展开 1 层，防止对象过大造成卡顿
- 修复中文无法展示的问题

## v2017-05-12

- Arthas 3.0 release

## v2016-12-09

- as.sh 支持-h 输出帮助
- [#121] 修复残留的临时文件导致 arthas 启动失败的问题
- [#123] 修复反复 attach/shutdown 造成 classloader 泄露的问题
- 优化命令中的帮助提示信息
- [#126] 修复 tm 命令文档链接错乱的问题
- [#122] classloader 命令中过滤掉`sun.reflect.DelegatingClassLoader`
- [#129] 修复 classloader 层次展示的问题
- [#125] arthas 输出的 log 不主动换行，对于日志解析更加友好
- [#96] sc 等命令支持 com/taobao/xxx/TestClass 这样的格式，以后复制粘贴不需要在把'/'替换成'.'啦
- [#124] 修复某些情况下 trace 的时间为负值的问题
- [#128] tt 命令的结果默认自动展开，不需要再增加`-x 2`来看到参数，异常的详细信息了。
- [#130] 修复当端口冲突时，没有很好地打印错误，而是进入了一个出错的交互界面的问题
- [#98] 修复 Arthas 启动时，如果下载更新失败，导致启动失败的问题
- [#139] 修复某些特殊情况下 agent attach 失败的问题
- [#156] jd-core-java 延迟初始化，避免 arthas 启动时出错
- 修复线程名重复的问题
- [#150] trace 命令支持按运行总耗时过滤
- 修复 sc 查找 SystemClassloader 时可能出现的 NPE
- [#180] 修复第一次 Attach 成功之后，删除之前 Arthas 的安装包，重新编译打包，再次 attach 失败的问题

## v2016-06-07

- 修复以资源方式加载 spy 类时出现 NPE 的问题
- 支持一键找出线程中获得锁并阻塞住其他线程的线程
- 优化 Thread 输出，按线程名排序
- 获取 topN 忙的线程时，支持指定刷新间隔

## v2016-04-08

- New feature：
  - dashboard 支持指定刷新频率，支持指定执行次数
  - 命令执行结果保存到日志文件，方便后续查看
  - 启动速度优化，第一次 attach 的速度提升一倍
  - 支持批处理功能，支持执行脚本文件
  - 优化启动逻辑，arthas 脚本启动时交互式选择进程
  - 类默认启用继承关系查询，查找类时默认会查找子类，如果需要关闭，则通过全局开关 Options disable-sub-class 关闭
  - 支持在彩色模式和文本模式中切换

- UI Improvement:
  - 合并 exit 和 quit 命令
  - 命令帮助信息增加 wiki 链接
  - 优化 watch 的逻辑，更加符合大家的直觉
  - thread 命令增加 example 说明
  - 自动补全的时候，忽略大小写

- Bugfix:
  - 修复 trace 命令遇到循环造成输出太长
  - 修复 trace 命令在方法调用中抛出了异常，会让 trace 的节点错位
  - 修正增强 BootstrapClassLoader 加载的类，找不到 Spy 的问题
  - 修复某些配色方案下，结果显示不友好的问题

## v2016-03-07

- 支持一键查看当前最忙的前 N 个线程及其堆栈
- 修复 openjdk 下启动 arthas 失败的问题（需要重新安装 as.sh）
- 一些体验优化

## v2016-01-18

- 优化 jad，实时 dump 内存 byte array，并使用 jd-core-java 反编译，支持`行号显示`
- 修复 tt 命令在监控与线程上下文相关的方法调用时，显示/重做等场景下的 bug

## v2016-01-08

- 修复一些 bug
  - jad NPE
  - watch/monitor NPE
  - 不需要转义
  - 数据统计问题修复
  - sc 查看静态变量内部层次结构

## v2015-12-29

- Arthas 2.0 测试版本发布！
