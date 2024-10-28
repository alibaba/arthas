---
id: "question-history-13522"
title: "Connect to telnet server error: 127.0.0.1 3658"
date: "2024-07-04"
category: "expertConsultation"
description: "遇到错误 arthas Connect to telnet server error: 127.0.0.1 3658 可能的原因及解决步骤如下：### 原因分析：1. **端口未开启**：Arthas 默认的 telnet 端口是 3658，该错误提示表示无法连接到这个端口，可能是因为 Arthas"
---

## 用户问题 ：

Connect to telnet server error: 127.0.0.1 3658

---

## 专家官方解答 ：

遇到错误 "arthas Connect to telnet server error: 127.0.0.1 3658" 可能的原因及解决步骤如下：

### 原因分析：

1. **端口未开启**：Arthas 默认的 telnet 端口是 3658，该错误提示表示无法连接到这个端口，可能是因为 Arthas 服务并未成功启动或者没有监听在该端口上。
2. **配置问题**：根据 Arthas 的配置规则，如果 `arthas.telnetPort` 被设置为 -1，则会禁止监听 telnet 端口；设置为 0 则会随机分配一个端口，而这个随机端口可能不是预期的 3658。
3. **权限问题**：尝试连接的用户可能没有足够的权限访问正在运行 Arthas 的 Java 进程。
4. **网络问题**：如果是在尝试远程连接，确保网络畅通，且没有防火墙或安全组规则阻止访问 3658 端口。

### 解决步骤：

1. **检查 Arthas 是否正确启动**：
   - 通过查看 Arthas 的启动日志（通常位于 `~/logs/arthas/arthas.log`），确认 Arthas 是否成功启动并且监听了 telnet 端口。如果发现端口被设置为非默认值，需使用正确的端口号尝试连接。
2. **检查配置**：
   - 查看 `arthas.properties` 文件（位于 `~/.arthas/lib/3.x.x/arthas/` 或解压目录下），确认 `arthas.telnetPort` 的设置是否符合预期。如有需要，修改此配置并重启 Arthas。
3. **权限验证**：
   - 确认执行连接命令的用户是否有权限访问目标 Java 进程。必要时，可以尝试使用具有足够权限的用户执行连接命令，或调整进程权限。
4. **网络与防火墙检查**：
   - 如果是远程连接，确保网络路径通畅，且两端的防火墙或安全组策略允许 3658 端口的通信。
5. **使用随机端口**（可选）：
   - 如果存在端口冲突问题，可以考虑设置 `arthas.telnetPort=0` 让 Arthas 自动分配一个可用端口，并通过日志查找实际使用的端口号。
6. **尝试其他连接方式**：
   - 如果 telnet 方式持续存在问题，考虑使用 HTTP 接口进行连接（如果已启用 `arthas.httpPort`）。

### 解释：

以上步骤旨在逐一排查并解决无法连接到 Arthas telnet 服务的问题，从最基本的启动状态检查到深入的配置与权限校验，每一步都是为了确保 Arthas 正确配置并能够被外部访问。通过这些步骤，应能定位并解决连接失败的问题。

<font color="#949494">---------------</font>

## 参考链接 ：

[Arthas Properties](../arthas-properties.md)

[Arthas Install](../install-detail.md)

[启动 Arthas](../start-arthas.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16077)给我们反馈。
