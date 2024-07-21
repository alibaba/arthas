---
id: "question-history-14840"
title: "Unsupported class file major version 61 using arthas 3.6.3"
date: "2024-07-04"
category: "expertConsultation"
description: "内容如下：1. **问题分析**：您遇到的问题“Unsupported class file major version 61 using arthas 3.6.3”表明您正在尝试使用Arthas 3.6.3版本来诊断一个使用Java 17编译的程序。错误信息中的class file major v"
---

## 用户问题 ：

Unsupported class file major version 61 using arthas 3.6.3

---

## 专家官方解答 ：

内容如下：

1. **问题分析**：您遇到的问题“Unsupported class file major version 61 using arthas 3.6.3”表明您正在尝试使用Arthas 3.6.3版本来诊断一个使用Java 17编译的程序。错误信息中的"class file major version 61"对应于Java SE 17的版本号。Arthas 3.6.3版本不直接支持Java 17，因此导致了不兼容性问题。

2. **解决方案依托于知识**：
   - 根据知识内容，我们知道**Arthas确实已经支持了JDK 17**。但是，要解决您当前的问题，需要确保您使用的Arthas版本是最新的，因为只有最新版本才可能包含了对JDK 17的支持。
3. **具体步骤**：
   - **升级Arthas到最新版本**：
     为了确保支持JDK 17，您应该按照以下步骤升级Arthas至最新版本：
     ```bash
     # 使用arthas-boot进行快速安装（推荐）
     curl -O https://arthas.aliyun.com/arthas-boot.jar
     java -jar arthas-boot.jar
     ```
     在启动arthas-boot时，如果有网络问题或者需要更快的下载速度，可以使用阿里云的镜像源：
     ```bash
     java -jar arthas-boot.jar --repo-mirror aliyun --use-http
     ```
     这一步骤将自动下载并启动最新版本的Arthas，从而解决您遇到的不兼容问题。
4. **解释**：
   - 通过升级Arthas到最新版本，您可以获得对更高版本Java（如JDK 17）的支持。这是因为Arthas团队不断更新其工具以适应最新的Java技术栈，确保开发者能够顺利诊断运行在最新Java版本上的应用程序。

如果您在执行上述步骤后仍然遇到问题，可能需要检查是否有其他环境因素干扰，或者考虑查阅Arthas的官方文档和社区论坛寻求进一步的帮助。

请注意，以上解决方案基于提供的知识内容和一般逻辑推理得出，旨在提供一种直接且有效的方法来解决您的问题。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：arthas支持jdk20吗？](./user-question-history14843.html)

[Arthas Install](../install-detail.md)

[专家经验：Arthas 介绍](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16083)给我们反馈。
