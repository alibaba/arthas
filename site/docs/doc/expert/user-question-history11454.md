---
id: "question-history-11454"
title: "arthas 常用命令介绍"
date: "2024-07-04"
category: "expertConsultation"
description: "Arthas是一款由Alibaba开源的强大Java诊断工具，它能够帮助开发者在不修改代码、不重启应用程序的前提下，对线上问题进行快速诊断和定位。以下是Arthas中一些常用命令的介绍，这些命令覆盖了从JVM监控、类加载信息查询到性能分析等多个方面：1. **JVM相关信息查询**   - **jv"
---

## 用户问题 ：

arthas 常用命令介绍

---

## 专家官方解答 ：

Arthas是一款由Alibaba开源的强大Java诊断工具，它能够帮助开发者在不修改代码、不重启应用程序的前提下，对线上问题进行快速诊断和定位。以下是Arthas中一些常用命令的介绍，这些命令覆盖了从JVM监控、类加载信息查询到性能分析等多个方面：

1. **JVM相关信息查询**
   - **jvm**: 查看当前JVM的基本信息。
   - **memory**: 查看JVM内存使用情况。
   - **sysprop**: 查看和修改JVM系统属性。
   - **sysenv**: 查看JVM环境变量。
   - **vmoption**: 查看和修改JVM诊断相关的选项。
   - **heapdump**: 生成Java堆转储文件，类似于jmap的功能。

2. **类与类加载器操作**
   - **sc**: 查看已加载的类信息。
   - **sm**: 查看类方法信息。
   - **classloader**: 查看类加载器的详细信息，包括类加载路径等。
   - **redefine/retransform**: 动态修改已加载类的字节码，分别用于重新定义和转换类。

3. **监控与性能分析**
   - **monitor**: 方法执行监控，可以统计方法调用次数、总耗时等。
   - **trace**: 方法内部调用路径分析，输出方法调用链路及其耗时。
   - **watch**: 实时观测方法执行数据，可在方法执行前后插入观测点。
   - **profiler**: 使用async-profiler生成火焰图，进行性能瓶颈分析。
   - **jfr**: 动态开启JDK Flight Recorder记录，进行高级性能分析。

4. **调试辅助**
   - **thread**: 查看当前JVM线程堆栈信息。
   - **stack**: 显示指定方法被调用的调用路径。
   - **jad**: 反编译已加载类的源码。
   - **mc**: 内存编译器，直接在JVM内存中编译Java源码为字节码。

5. **系统操作与辅助命令**
   - **base64/grep/cat/echo**: 类似Linux命令，提供文本处理、编码转换等功能。
   - **history**: 查看命令历史。
   - **session**: 查看当前会话信息。
   - **auth**: 鉴权管理，确保操作安全。
   - **jobs/kill/fg/bg**: 管理后台异步任务，适合长时间监控场景。

6. **其它实用功能**
   - **logger**: 查看和修改日志配置。
   - **mbean**: 查看MBean信息，用于JMX管理。
   - **vmtool**: 在JVM中查询对象、执行强制GC等操作。
   - **options**: 查看或设置Arthas全局开关。
   - **dump**: 将已加载类的字节码dump到磁盘。

这些命令的综合运用能极大地提高问题定位和排查效率，让Java应用的线上诊断变得更为直观和高效。更多详细信息和使用示例，可以通过查阅[Arthas官方文档](https://arthas.aliyun.com/doc/)获得。

<font color="#949494">---------------</font>

## 参考链接 ：

[命令列表](../commands.md)

[专家经验：Arthas 介绍](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16048)给我们反馈。
