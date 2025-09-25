---
id: "question-history-11784"
title: "arthas使用的各类方式有哪些？"
date: "2024-07-04"
category: "expertConsultation"
description: "Arthas 提供了多种实用的功能来帮助开发者诊断和解决问题，以下是 Arthas 使用的一些主要方式：1. **启动与连接**   - **交互模式启动**：直接运行 `./as.sh` 脚本（Linux/Unix/Mac），它会列出当前系统中运行的Java进程并让你选择一个进行连接。   - *"
---

## 用户问题 ：

arthas使用的各类方式有哪些？

---

## 专家官方解答 ：

Arthas 提供了多种实用的功能来帮助开发者诊断和解决问题，以下是 Arthas 使用的一些主要方式：

1. **启动与连接**
   - **交互模式启动**：直接运行 `./as.sh` 脚本（Linux/Unix/Mac），它会列出当前系统中运行的Java进程并让你选择一个进行连接。
   - **非交互模式启动**：通过指定PID（进程ID）以及可选的IP和PORT来直接连接到目标Java进程，如 `./as.sh 12345`。对于远程诊断，可以在目标服务器上启动Arthas Server后，通过`telnet IP PORT`来远程连接。

2. **查看与分析**
   - **类加载信息查询**：使用 `sc` 或 `sm` 命令查看类的加载信息或方法详情，帮助识别类加载异常或了解类的方法结构。
   - **线程堆栈分析**：使用 `thread` 或 `tt` 命令查看线程堆栈，快速定位死锁或线程异常。
   - **性能监控**：利用 `perf`、`monitor`、`watch` 等命令监控方法调用频率、耗时及堆栈跟踪，甚至生成火焰图来直观展示热点路径。

3. **诊断与调试**
   - **代码热更新**：`redefine` 命令允许你在线修改类的定义，无需重启应用。
   - **条件断点**：使用 `debug` 命令设置条件断点，当特定条件满足时暂停程序执行，非常适用于难以复现的问题。
   - **表达式求值**：通过 `expr` 命令在JVM中直接执行Java表达式，查看变量值或调用方法。

4. **系统信息查看**
   - **JVM信息**：`jvm` 命令提供JVM的概览信息，包括内存、垃圾收集器状态等。
   - **系统环境**：使用 `sysprop` 查看系统属性，`env` 查看环境变量。

5. **其他高级功能**
   - **追踪方法调用链路**：`trace` 命令可以追踪指定方法的调用路径，帮助理解复杂调用关系。
   - **实例搜索**：`dashboard` 和 `objsearch` 命令可以全局搜索类实例，便于定位特定对象的状态。

为了充分利用Arthas，建议根据具体需求查阅其详细文档，比如通过提供的[离线帮助文档](https://arthas.aliyun.com/download/doc/latest_version?mirror=aliyun)深入了解每个命令的使用方法。此外，Arthas的安装方式多样，包括使用`arthas-boot.jar`快速启动、通过脚本`as.sh`一键安装、全量安装、甚至集成到IDE中使用，确保了在不同场景下的灵活性和便捷性。

综上所述，Arthas的使用方式覆盖了从基本的Java进程连接、诊断到高级的性能分析与调试，是Java开发者不可或缺的诊断利器。

<font color="#949494">---------------</font>

## 参考链接 ：

[Arthas Install](../install-detail.md)

[启动 Arthas](../start-arthas.md)

[专家经验：Arthas 介绍](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16053)给我们反馈。
