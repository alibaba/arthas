---
id: "question-history-14834"
title: "使用arthas 跟踪进程服务后，进程所占用的文件描述符陡增，且文件描述符不下降"
date: "2024-07-04"
category: "expertConsultation"
description: "在您使用Arthas跟踪进程服务后遇到进程所占用的文件描述符陡增且不下降的问题，根据已有知识，这是由于Arthas在非正常关闭时（如使用`ctrl + z`或`exit`命令退出）没有正确释放文件描述符所致。为了解决此问题并避免资源泄露，请按照以下步骤操作：1. **正确停止Arthas**    "
---

## 用户问题 ：

使用arthas 跟踪进程服务后，进程所占用的文件描述符陡增，且文件描述符不下降

---

## 专家官方解答 ：

在您使用Arthas跟踪进程服务后遇到进程所占用的文件描述符陡增且不下降的问题，根据已有知识，这是由于Arthas在非正常关闭时（如使用`ctrl + z`或`exit`命令退出）没有正确释放文件描述符所致。为了解决此问题并避免资源泄露，请按照以下步骤操作：

1. **正确停止Arthas**  
   当你需要结束Arthas会话时，不应直接使用`ctrl + z`或者输入`exit`命令退出。正确的做法是，在Arthas命令行中输入`stop`命令来确保Arthas及其相关的文件描述符被正确关闭和释放。

2. **验证文件描述符释放情况**  
   在执行`stop`命令后，可以通过监控工具或JVM参数（如检查`OPEN-FILE-DESCRIPTOR-COUNT`）来验证进程的文件描述符数量是否已经下降到正常水平。这有助于确认问题是否得到有效解决。

3. **调整MAX-FILE-DESCRIPTOR-COUNT（如有必要）**  
   如果在日常运维中频繁遇到文件描述符不足的问题，考虑调整JVM启动参数中的`MAX-FILE-DESCRIPTOR-COUNT`，以增加进程可打开的最大文件描述符数量，但需注意不要设置得过高以免影响系统稳定性。

**解释说明：**

- **为何要使用`stop`命令**：`stop`命令是Arthas提供的标准退出方式，它能确保不仅Arthas自身进程结束，还会妥善清理由Arthas创建的所有资源，包括但不限于文件描述符。
- **监控文件描述符的重要性**：及时监控和管理文件描述符的使用情况对于维护系统稳定性和避免资源耗尽至关重要，尤其是在长时间运行的服务和诊断工具使用场景下。
- **调整MAX-FILE-DESCRIPTOR-COUNT的考量**：虽然调整最大文件描述符限制可以临时缓解问题，根本上还是需要确保应用程序和辅助工具如Arthas能够正确管理资源，避免不必要的资源占用。

如果您在操作过程中还有其他疑问，或者问题未能得到解决，建议查阅Arthas的官方教程，其中提供了更为详尽的操作指南和故障排查方法：

- [Arthas官方教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn)

希望以上步骤能帮助您有效解决文件描述符不下降的问题。

<font color="#949494">---------------</font>

## 参考链接 ：

专家经验：使用arthas 跟踪进程服务后，进程所占用的文件描述符陡增，且文件描述符不下降

[jvm](../jvm.md)

专家经验：Arthas 官方教程

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16082)给我们反馈。
