---
id: "question-history-11450"
title: "arthas 分析thread线程卡住"
date: "2024-07-04"
category: "expertConsultation"
description: "要使用Arthas分析线程（thread）卡住的问题，我们可以按照以下步骤操作：### 1. 识别问题原因根据提供的知识，当应用发生卡顿时，往往是由于某个线程持有锁，导致其他线程等待该锁而无法继续执行。Arthas提供了`thread -b`命令来帮助定位这个造成阻塞的线程。### 2. 使用Art"
---

## 用户问题 ：

arthas 分析thread线程卡住

---

## 专家官方解答 ：

要使用Arthas分析线程（thread）卡住的问题，我们可以按照以下步骤操作：

### 1. 识别问题原因

根据提供的知识，当应用发生卡顿时，往往是由于某个线程持有锁，导致其他线程等待该锁而无法继续执行。Arthas提供了`thread -b`命令来帮助定位这个造成阻塞的线程。

### 2. 使用Arthas查找阻塞线程

执行命令：

```bash
thread -b
```

此命令会列出当前阻塞其他线程的线程信息，包括线程ID、状态以及它持有的锁。例如，输出可能会显示某个线程处于TIMED_WAITING状态，并指出该线程持有一个锁，同时阻塞了其他线程。

### 3. 分析输出

- **关注锁定对象**：注意输出中被`- locked`标记的行，这表示该线程持有的锁。如果该锁导致了其他线程的阻塞，这里会有明确提示。
- **查看调用栈**：调用栈信息可以帮助理解该线程在阻塞前正在执行的操作，这对于诊断问题至关重要。

### 注意事项

- 目前`thread -b`仅支持`synchronized`关键字导致的线程阻塞分析，对于使用`java.util.concurrent.Lock`的情况暂不支持。
- 如果需要对每个线程分别进行性能分析（profiling），可以使用`profiler start -t`命令，但请注意这一步骤主要用于性能监控，而非直接解决线程阻塞问题。

### 链接与资源

- [Arthas官方教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn)提供了更全面的使用指南和高级功能介绍。

综上所述，通过Arthas的`thread -b`命令能有效识别并分析导致应用卡住的线程阻塞问题，为解决问题提供关键信息。进一步的解决方案则需基于具体分析结果进行，比如优化锁定策略或代码逻辑等。

<font color="#949494">---------------</font>

## 参考链接 ：

[thread](../thread.md)

[专家经验：Arthas 官方教程](./user-question-history11466.md)

[profiler](../profiler.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16044)给我们反馈。
