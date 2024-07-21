---
id: "question-history-11440"
title: "arthas 怎么使用？"
date: "2024-07-04"
category: "expertConsultation"
description: "Arthas 是一个Java诊断工具，它提供了丰富的命令来帮助开发者诊断应用程序的运行时问题。以下是如何使用 Arthas 的简要指南，基于你提供的信息：### 启动 Arthas1. **安装 Arthas**：首先，确保你已经安装了 Arthas。如果尚未安装，可以通过下载其发布包或使用 Hom"
---

## 用户问题 ：

arthas 怎么使用？

---

## 专家官方解答 ：

Arthas 是一个Java诊断工具，它提供了丰富的命令来帮助开发者诊断应用程序的运行时问题。以下是如何使用 Arthas 的简要指南，基于你提供的信息：

### 启动 Arthas

1. **安装 Arthas**：首先，确保你已经安装了 Arthas。如果尚未安装，可以通过下载其发布包或使用 Homebrew（针对 macOS 用户）等方式安装。具体安装步骤可参考 Arthas 的官方文档。

2. **连接到目标 Java 应用**：启动 Arthas 后，你需要连接到你想诊断的 Java 进程。通过命令行输入 `java -jar arthas-boot.jar`，它会列出所有正在运行的 Java 进程。选择你想连接的进程编号并输入，即可进入 Arthas 的交互界面。

### 使用 `trace` 命令

一旦进入 Arthas 的交互界面，你可以使用 `trace` 命令来跟踪方法的调用链路及耗时，这对于定位性能瓶颈或理解代码执行流程非常有帮助。

#### 基本使用

- **跟踪单个方法**：假设你要跟踪 `demo.MathGame` 类中的 `run` 方法，输入命令 `trace demo.MathGame run`。这会显示每次 `run` 方法被调用时的详细堆栈信息，包括调用时间、线程信息以及内部调用的方法等。

- **限制输出**：如果方法调用频繁，可以使用 `-n` 参数限制输出次数，如 `trace demo.MathGame run -n 1` 仅追踪一次调用后即停止。

- **查看调用耗时**：通过添加条件过滤，如 `trace demo.MathGame run '#cost > 10'`，仅展示耗时超过10毫秒的调用路径。

- **包含/排除特定类或方法**：使用 `-E` 参数匹配多个类或方法，例如 `trace -E com.test.ClassA|org.test.ClassB method1|method2|method3`。使用 `--exclude-class-pattern` 来排除特定类，如 `trace javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter`。

#### 高级选项

- **指定最大类/方法匹配数量**：使用 `-m` 参数限制追踪的类或方法数量，例如 `trace demo.MathGame run -m 1`。

- **包含 JDK 方法**：默认情况下，`trace` 不会追踪 JDK 内部的方法。若需包含，使用 `--skipJDKMethod false`，如 `trace --skipJDKMethod false demo.MathGame run`。

### 结果解释

- 报告中的时间戳（如 `[12.033735ms]`）表示该方法调用的耗时。
- 调用次数统计（如 `0,0,0ms,11`）展示了方法调用的耗时分布，包括最小、最大耗时、总耗时和调用次数，以及是否存在异常信息。

综上所述，利用 Arthas 的 `trace` 命令，你可以有效地监控和诊断 Java 应用程序中的方法调用行为，进而定位性能问题或理解程序的执行流程。请根据实际需要灵活运用上述命令和选项。

<font color="#949494">---------------</font>

## 参考链接 ：

[trace](../trace.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16040)给我们反馈。
