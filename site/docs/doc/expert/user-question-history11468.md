---
id: "question-history-11468"
title: "arthas 怎么使用？"
date: "2024-07-04"
category: "expertConsultation"
description: "Arthas 的使用主要分为两个阶段：安装与启动。接下来我将依据提供的知识内容，分步骤向您介绍如何使用 Arthas。### 安装 Arthas**方法一：使用 arthas-boot（推荐）**1. **下载 arthas-boot.jar**   执行以下命令下载 arthas-boot.jar"
---

## 用户问题 ：

arthas 怎么使用？

---

## 专家官方解答 ：

Arthas 的使用主要分为两个阶段：安装与启动。接下来我将依据提供的知识内容，分步骤向您介绍如何使用 Arthas。

### 安装 Arthas

**方法一：使用 arthas-boot（推荐）**

1. **下载 arthas-boot.jar**
   执行以下命令下载 arthas-boot.jar 文件：

   ```bash
   curl -O https://arthas.aliyun.com/arthas-boot.jar
   ```

   如果下载速度慢，可以尝试阿里云镜像：

   ```bash
   java -jar arthas-boot.jar --repo-mirror aliyun --use-http
   ```

2. **启动 arthas-boot**
   通过 Java 命令直接运行 arthas-boot.jar：
   ```bash
   java -jar arthas-boot.jar
   ```
   若要查看帮助信息，执行：
   ```bash
   java -jar arthas-boot.jar -h
   ```

**方法二：使用 `as.sh` 脚本**

1. **一键安装**
   在命令行中执行以下命令下载并安装启动脚本 `as.sh`：
   ```bash
   curl -L https://arthas.aliyun.com/install.sh | sh
   ```
   然后通过 `./as.sh` 命令启动 Arthas。

**方法三：全量/手动安装**

- 访问 [Arthas 最新版本下载页面](https://arthas.aliyun.com/download/latest_version?mirror=aliyun)，下载 arthas-packaging.zip。
- 解压后，使用 `java -jar arthas-boot.jar` 命令启动。

**方法四：通过 rpm/deb 安装**

- 下载 rpm/deb 包。
- 使用 `sudo dpkg -i arthas*.deb` 或 `sudo rpm -i arthas*.rpm` 安装。

### 启动 Arthas

**交互模式启动**

- 直接运行 `./as.sh`（或在 Windows 中运行 `as.bat`），它会列出当前系统中的 Java 进程供你选择，输入进程号后回车即可附加到该进程。

**非交互模式启动**

- 通过指定进程ID (PID) 及可选的 IP 和 PORT 启动：
  ```bash
  ./as.sh <PID>[@IP:PORT]
  ```
  如不指定，默认为本地 (`127.0.0.1:3658`)。

**远程诊断**

- 在服务器上启动 Arthas Server 后，可通过 `telnet IP PORT` 远程连接进行诊断。

### Arthas 功能简介

Arthas 提供了多种功能来帮助你诊断 Java 应用，包括但不限于：

- 查看类加载信息
- 动态追踪方法调用
- 实时代码热更新
- 监控方法执行耗时（生成火焰图）
- 查找类实例
- JVM性能监控等

以上步骤涵盖了安装、启动及简要介绍了 Arthas 的强大功能。根据具体需求，您可以进一步探索 Arthas 的详细命令和高级用法。

<font color="#949494">---------------</font>

## 参考链接 ：

[Arthas Install](../install-detail.md)

[启动 Arthas](../start-arthas.md)

[专家经验：Arthas 介绍](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16051)给我们反馈。
