---
id: "question-history-13506"
title: "如何查看java内存增长哪些对象"
date: "2024-07-04"
category: "expertConsultation"
description: "要使用Arthas查看Java内存中哪些对象在增长，我们可以按照以下步骤操作：### 1. 初始检测内存状态首先，使用Arthas的`memory`命令来查看当前JVM的内存概况，这能帮助我们初步判断是否有内存泄漏迹象及可能的泄漏区域。```bash$ memory```此命令会返回各个内存区域的使"
---

## 用户问题 ：

如何查看java内存增长哪些对象

---

## 专家官方解答 ：

要使用Arthas查看Java内存中哪些对象在增长，我们可以按照以下步骤操作：

### 1. 初始检测内存状态

首先，使用Arthas的`memory`命令来查看当前JVM的内存概况，这能帮助我们初步判断是否有内存泄漏迹象及可能的泄漏区域。

```bash
$ memory
```

此命令会返回各个内存区域的使用情况，如堆、非堆、各代空间等。注意观察哪些区域的使用率异常高或持续增长。

### 2. 定期监控内存变化

多次执行`memory`命令并记录结果，关注任何内存区域的使用量是否持续增长。特别是heap、old gen等区域，这些通常是内存泄漏发生的热点。

### 3. 分析堆内存

为了具体定位到增长的对象，需要使用`heapdump`命令生成堆转储文件。

```bash
[arthas@58205]$ heapdump arthas-output/dump.hprof
```

这会在指定位置（默认或自定义）生成一个`.hprof`文件，包含了堆内存中的所有对象信息。

### 4. 分析堆转储文件

使用像Eclipse Memory Analyzer (MAT)这样的工具分析`.hprof`文件。重点在于找出哪些类的对象数量异常增多或占用内存显著增长。

- 打开MAT，导入`.hprof`文件。
- 使用“Leak Suspects Report”或手动分析，查找内存泄漏的嫌疑对象。
- 重点关注“Dominator Tree”（支配树），它展示了哪些对象占用了大量内存。
- 利用“Histogram”视图查看各类对象的数量和大小分布。

### 5. 使用`--live`标志优化分析

如果想专注于未被垃圾回收的对象，可以在使用分析工具时加上`--live`标志，以过滤掉那些无害的对象。

### 解释

通过上述步骤，我们利用Arthas初步检测内存使用情况，随后通过堆转储分析精确定位到导致内存增长的具体对象类型和数量。这样可以有效地识别出内存泄漏的根源，为进一步优化和修复提供依据。请注意，定期进行内存状态监测对于发现潜在的内存泄漏至关重要，因为内存泄漏问题往往随时间逐渐显现。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：Arthas 排查内存泄露的方法](./user-question-history12455.md)

[专家经验：Java 堆内存说明](./user-question-history12716.md)

[profiler](../profiler.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16069)给我们反馈。
