---
id: "question-history-15735"
title: "arthas报错connect to telnet server error: 127.0.0.1 9999"
date: "2024-07-04"
category: "expertConsultation"
description: "根据你提供的错误信息`connect to telnet server error: 127.0.0.1 9999`，这表明Arthas尝试连接到Telnet服务器时遇到了问题，具体来说是无法连接到本地的9999端口。结合我掌握的知识，我们可以分步分析并尝试解决这个问题：### 分析原因1. **端"
---

## 用户问题 ：

arthas报错connect to telnet server error: 127.0.0.1 9999

---

## 专家官方解答 ：

根据你提供的错误信息`connect to telnet server error: 127.0.0.1 9999`，这表明Arthas尝试连接到Telnet服务器时遇到了问题，具体来说是无法连接到本地的9999端口。结合我掌握的知识，我们可以分步分析并尝试解决这个问题：

### 分析原因

1. **端口未开启**：可能是因为Arthas配置的Telnet端口9999没有被正确监听。这可能是因为Arthas启动时没有使用这个端口，或者该端口被其他应用占用。
2. **配置错误**：Arthas的配置文件`arthas.properties`中关于`arthas.telnetPort`的设置可能不是9999，或者有其他配置覆盖了这一设定。
3. **网络问题或权限问题**：尽管不太常见，但也不排除本地网络配置阻止了连接，或者进程没有足够的权限监听该端口。

### 解决步骤

#### 步骤1：检查Arthas启动参数和配置

- 查看Arthas启动时是否明确指定了`--telnet-port 9999`。如果没有，查看`arthas.properties`文件（位于`~/.arthas/lib/3.x.x/arthas/`或解压目录下）中`arthas.telnetPort`的值是否为9999。
- 确认没有其他系统环境变量或系统属性覆盖了`arthas.telnetPort`的配置。

#### 步骤2：检查端口占用

- 使用命令（如`netstat -tuln | grep 9999` on Linux/macOS 或 `netstat -ano | findstr 9999` on Windows）检查9999端口是否已被其他程序占用。
- 如果端口被占用，结束占用该端口的进程或更改Arthas的telnet端口号。

#### 步骤3：重新启动Arthas

- 根据上述检查结果，如果需要修改配置或端口号，调整后重新启动Arthas。可以使用特定的端口号启动，例如：`./as.sh --telnet-port 9999`（确保该端口当前未被占用）。

#### 步骤4：验证连接

- 再次尝试连接到Telnet服务器，确认问题是否解决。

### 解释

以上步骤旨在定位并解决端口配置不当或被占用的问题，这是导致你遇到错误的主要原因。通过检查和调整配置，确保Arthas能够正常监听预期的端口，从而成功建立连接。

如果问题依旧存在，可能需要进一步检查Arthas的日志文件（默认位于`~/logs/arthas/arthas.log`），以获取更详细的错误信息来辅助诊断。

由于提供的知识内容没有直接提及解决特定的连接错误步骤，上述建议基于通用的故障排查逻辑。如果问题依然未能解决，可能需要更详细的应用日志或环境信息来进行深入分析。

<font color="#949494">---------------</font>

## 参考链接 ：

[Arthas Tunnel](../tunnel.md)

[专家经验：arthas 你好](./user-question-history11442.md)

[Arthas Properties](../arthas-properties.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16092)给我们反馈。
