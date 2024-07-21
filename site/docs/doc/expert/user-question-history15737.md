---
id: "question-history-15737"
title: "进程停了 arthas也会同步停止么？"
date: "2024-07-04"
category: "expertConsultation"
description: "根据已有的知识，我可以明确您的问题。****1. **问题分析：** Arthas 是一个运行在Java应用程序内部的诊断工具。它需要依附于目标Java进程来执行各种监控和诊断任务。2. **结论：** 当目标Java进程终止时，Arthas 也随之停止运行。因为Arthas是作为该进程的一部分运行"
---

## 用户问题 ：

进程停了 arthas也会同步停止么？

---

## 专家官方解答 ：

根据已有的知识，我可以明确您的问题。

---

1. **问题分析：** Arthas 是一个运行在Java应用程序内部的诊断工具。它需要依附于目标Java进程来执行各种监控和诊断任务。
2. **结论：** 当目标Java进程终止时，Arthas 也随之停止运行。因为Arthas是作为该进程的一部分运行的，所以宿主进程的结束会导致Arthas失去运行环境，从而自动关闭。

此外，如果您想了解更多关于Arthas的使用方法和教程，可以参考官方提供的详细文档：

- [Arthas官方教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn)

希望这些信息能帮助您解决问题。如果您有其他关于Arthas的疑问或需要进一步的帮助，请随时告诉我。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：Arthas 官方教程](./user-question-history11466.md)

[专家经验：arthas 你好](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16094)给我们反馈。
