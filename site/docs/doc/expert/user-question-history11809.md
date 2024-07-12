---
id: "question-history-11809"
title: "linux下的arthas 具体怎么用？（使用教程）"
date: "2024-07-04"
category: "expertConsultation"
description: "在Linux下使用Arthas的具体教程可以按照以下步骤进行：**1. 下载Arthas**推荐使用arthas-boot进行安装，这是一个便捷的启动器，能自动完成版本选择和环境检测。```bashcurl -O https://arthas.aliyun.com/arthas-boot.jar``"
---

## 用户问题 ：

linux下的arthas 具体怎么用？（使用教程）

---

## 专家官方解答 ：

在Linux下使用Arthas的具体教程可以按照以下步骤进行：

**1. 下载Arthas**

推荐使用arthas-boot进行安装，这是一个便捷的启动器，能自动完成版本选择和环境检测。

```bash
curl -O https://arthas.aliyun.com/arthas-boot.jar
```

**2. 运行arthas-boot并选择目标Java进程**

执行以下命令后，arthas-boot会列出当前系统中运行的所有Java进程供你选择：

```bash
java -jar arthas-boot.jar
```

屏幕上会显示如下的进程列表（示例）：

```
* [1]: 35542
  [2]: 71560 math-game.jar（例子）
```

根据需要选择一个进程编号并输入，例如输入“2”来选择math-game.jar这个进程。

**3. 连接成功并开始使用Arthas**

一旦连接成功，你将看到类似于以下的日志输出，表明Arthas已经成功接入目标Java应用：

```bash
[INFO] Try to attach process 71560
[INFO] Attach process 71560 success.
[INFO] arthas-client connect 127.0.0.1 3658
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


wiki: https://arthas.aliyun.com/doc
version: 3.0.5.20181127201536
pid: 71560
time: 2018-11-28 19:16:24
```

**4. 使用Arthas命令**

现在你可以开始使用Arthas提供的丰富功能了。比如查看当前应用的实时信息面板，只需输入：

```bash
dashboard
```

这会展示出包括JVM信息、线程堆栈、内存状况等在内的关键数据。

**5. 探索更多功能**

Arthas提供了诸如`trace`、`watch`、`monitor`、`heapdump`等多种命令，分别用于方法追踪、条件断点、性能监视及堆内存dump等。详细命令列表和使用方法可以通过访问其官方文档获取：

- [Arthas官方教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn)

**解释说明：**

以上步骤指导你在Linux环境下如何快速部署并开始使用Arthas。Arthas是一个强大的Java诊断工具，它能够帮助你无需重启应用即可进行问题诊断、性能监控等操作，极大地提高了开发和运维效率。通过跟随上述步骤，你可以轻松地接入Arthas并探索其强大功能，为你的Java应用保驾护航。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：Arthas 官方教程](./user-question-history11466.md)

[专家经验：Arthas 介绍](./user-question-history11442.md)

[专家经验：Arthas 快速启动](./user-question-history11440.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16056)给我们反馈。
