---
id: "question-history-11443"
title: "arthas 下载和安装"
date: "2024-07-04"
category: "expertConsultation"
description: "内容要有条理，分步骤：1. **理解问题**：您想了解的是如何下载和安装Arthas。Arthas是一款阿里巴巴开源的Java诊断工具，它可以帮助您监控、诊断和分析Java应用程序的运行状态。2. **推荐安装方法**：根据提供的知识，最简便的安装方法是使用arthas-boot进行安装。   **"
---

## 用户问题 ：

arthas 下载和安装

---

## 专家官方解答 ：

内容要有条理，分步骤：

1. **理解问题**：您想了解的是如何下载和安装Arthas。Arthas是一款阿里巴巴开源的Java诊断工具，它可以帮助您监控、诊断和分析Java应用程序的运行状态。

2. **推荐安装方法**：根据提供的知识，最简便的安装方法是使用arthas-boot进行安装。

   **具体步骤如下**：
   - **下载arthas-boot**：
     打开终端或命令提示符，输入以下命令下载arthas-boot.jar：
     ```bash
     curl -O https://arthas.aliyun.com/arthas-boot.jar
     ```
   - **启动并选择目标进程**：
     接着，运行以下命令启动arthas-boot，并按照提示选择您想要诊断的Java进程：
     ```bash
     java -jar arthas-boot.jar
     ```
     例如，如果列表显示`[2]: 71560 math-game.jar`，输入数字2并回车，将Arthas挂载到math-game.jar进程上。
   - **使用Arthas**：
     成功安装并挂载后，您可以开始使用Arthas的各种命令，如输入`dashboard`查看实时数据面板，验证Arthas是否正确安装并运行。

3. **其他安装方式**：除了上述推荐方法，还有手动安装方式，包括下载arthas-packaging-bin.zip，解压，然后通过脚本安装到本地。这种方式适合需要更定制化安装的场景。具体步骤在知识中也有详细说明，但考虑到便捷性和普遍适用性，优先推荐使用arthas-boot方式。

4. **Arthas Tunnel Server**：如果您还需要远程连接到Arthas，可以下载并部署arthas tunnel server，它提供了Web界面以便您通过网络连接到不同机器上的Arthas代理。启动命令简单为：
   ```bash
   java -jar arthas-tunnel-server.jar
   ```
   默认情况下，Web端口为8080，代理连接端口为7777。

通过上述步骤，您可以顺利完成Arthas的下载和安装，并根据需要选择是否配置远程连接功能。请根据您的具体需求选择合适的安装方式。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：Arthas 快速启动](./user-question-history11440.md)

[手动安装 Arthas](../manual-install.md)

[Arthas Tunnel](../tunnel.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16042)给我们反馈。
