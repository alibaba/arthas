---
id: "question-history-14763"
title: "利用Arthas排查NoSuchMethodError"
date: "2024-07-04"
category: "expertConsultation"
description: "利用Arthas排查`NoSuchMethodError`的问题可以按照以下步骤进行：1. **识别异常信息**：当遇到如`java.lang.NoSuchMethodError: org.springframework.core.annotation.AnnotationAwareOrderCom"
---

## 用户问题 ：

利用Arthas排查NoSuchMethodError

---

## 专家官方解答 ：

利用Arthas排查`NoSuchMethodError`的问题可以按照以下步骤进行：

1. **识别异常信息**：当遇到如`java.lang.NoSuchMethodError: org.springframework.core.annotation.AnnotationAwareOrderComparator.sort(Ljava/util/List;)V`这样的错误时，首先要仔细阅读异常信息，它告诉你`AnnotationAwareOrderComparator`类中缺少了一个特定的方法签名`sort(List)`。

2. **接入Arthas**：确保你的应用虽然抛出异常但并未完全退出。接下来，启动Arthas工具。在Mac环境下，可以通过命令行执行`./as.sh`（确保Arthas已安装并可执行）。

3. **选择目标进程**：在Arthas的交互界面，使用`dashboard`或直接输入进程名（如上述示例中的`com.example.demoNoSuchMethodError.DemoNoSuchMethodErrorApplication`）并回车，来选择需要诊断的Java进程。

4. **查找类来源**：使用Arthas的`sc`命令（即`$ sc -d org.springframework.core.annotation.AnnotationAwareOrderComparator`）来查找问题类的加载信息。这一步帮助我们确认该类是从哪个JAR包加载的，如例子所示，问题是由于从spring-2.5.6.SEC03.jar加载了不兼容版本的`AnnotationAwareOrderComparator`。

5. **反编译源码确认**：为了进一步确认问题，可以使用`jad`命令（如`$ jad org.springframework.core.annotation.AnnotationAwareOrderComparator`）查看该类的反编译源码。这步验证了类确实没有预期的方法，正如异常所指出的那样。

6. **分析依赖并解决问题**：根据Arthas提供的信息，发现冲突来源于spring-2.5.6.SEC03.jar。因此，回到你的项目构建配置（如Maven或Gradle），检查并排除这个旧版本的Spring依赖，确保使用一个与应用其他部分兼容的Spring版本。完成依赖调整后，重新构建和部署应用。

**解释**：

- 通过Arthas，我们能迅速定位到问题类的加载位置，这是解决`NoSuchMethodError`的关键一步，因为它通常由不同版本的库引起。
- 反编译源码是为了直接验证类缺失方法的假设，进一步巩固我们的判断。
- 排除不兼容的依赖是根本的解决策略，避免了因版本冲突导致的运行时错误。

综上所述，Arthas作为一个强大的Java诊断工具，在排查此类问题时提供了高效的手段，从识别问题到定位原因，再到指导修复，形成了完整的解决方案链路。

<font color="#949494">---------------</font>

## 参考链接 ：

[专家经验：Arthas 介绍](./user-question-history11442.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16078)给我们反馈。
