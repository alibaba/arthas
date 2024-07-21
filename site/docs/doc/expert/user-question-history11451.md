---
id: "question-history-11451"
title: "arthas 怎么排查cpu占用过高？"
date: "2024-07-04"
category: "expertConsultation"
description: "为了排查CPU占用过高的问题，我们可以通过以下步骤利用Arthas进行诊断，这些步骤依据于已知的知识内容：**第一步：确保Arthas环境准备就绪**- 首先，确保你已经下载并安装了Arthas，且拥有正确的权限能够attach到目标的Java进程中。如果尚未安装，可以通过推荐的arthas-boo"
---

## 用户问题 ：

arthas 怎么排查cpu占用过高？

---

## 专家官方解答 ：

为了排查CPU占用过高的问题，我们可以通过以下步骤利用Arthas进行诊断，这些步骤依据于已知的知识内容：

**第一步：确保Arthas环境准备就绪**

- 首先，确保你已经下载并安装了Arthas，且拥有正确的权限能够attach到目标的Java进程中。如果尚未安装，可以通过推荐的arthas-boot方式进行安装，具体命令为：
  ```bash
  curl -O https://arthas.aliyun.com/arthas-boot.jar
  ```
- 接着，通过运行`java -jar arthas-boot.jar`选择需要挂载的目标Java进程。

**第二步：排除GC影响**

- 运行`dashboard`命令，关注屏幕中间Memory相关的信息。检查内存使用情况（usage）是否已满以及GC（垃圾回收）频率是否过高。如果GC频繁，可能说明存在内存泄露或不当的内存使用模式导致CPU被频繁用于GC操作。

**第三步：定位高CPU消耗的线程**

- 继续使用`dashboard`命令，这次重点关注屏幕上部的进程信息，识别出哪些线程占据了较高的CPU资源。思考这些线程与程序的哪部分逻辑相关联。

**第四步：深入分析高CPU消耗线程**

- 执行`thread -n 8`命令来查看最繁忙的8个线程的堆栈信息。这些堆栈信息会展示线程当前主要停留在哪个方法上执行，从而帮助定位到CPU消耗较多的具体方法或代码块。

**解释上述步骤：**

- 通过排除GC影响，我们首先确定问题是否由内存管理不当引起，因为频繁的GC活动也会占用大量CPU资源。
- 定位到高CPU消耗的线程后，我们能更有针对性地调查问题，而不是盲目地在整个应用中搜索。
- 深入分析线程堆栈是关键一步，它直接指向了问题发生的代码位置，为后续的代码优化或问题修复提供了准确的方向。

综上所述，利用Arthas的这一系列命令，你可以逐步缩小问题范围，最终定位到造成CPU占用过高的具体原因，并据此采取相应的优化措施。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：Arthas 快速启动](./user-question-history11440.md)

[专家经验：Arthas 介绍](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16045)给我们反馈。
