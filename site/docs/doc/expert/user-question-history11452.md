---
id: "question-history-11452"
title: "arthas 查看方法耗时"
date: "2024-07-04"
category: "expertConsultation"
description: "要使用Arthas来查看方法的耗时，可以依据提供的知识进行以下操作：1. **启动Arthas并连接到目标Java进程**     首先，你需要启动Arthas工具并连接到你想要监控的Java应用程序。在命令行中，运行Arthas的启动脚本（例如在Mac或Linux环境下通常是`./as.sh`），"
---

## 用户问题 ：

arthas 查看方法耗时

---

## 专家官方解答 ：

要使用Arthas来查看方法的耗时，可以依据提供的知识进行以下操作：

1. **启动Arthas并连接到目标Java进程**  
   首先，你需要启动Arthas工具并连接到你想要监控的Java应用程序。在命令行中，运行Arthas的启动脚本（例如在Mac或Linux环境下通常是`./as.sh`），然后从列出的Java进程中选择你要附加的进程ID。

2. **使用`trace`命令监控方法调用**  
   接下来，利用Arthas的`trace`命令来自动高亮显示最耗时的方法调用。这是一个非常实用的功能，它能帮助你快速定位到性能瓶颈。命令的基本格式如下：

   ```shell
   trace [class-pattern] [method-pattern] [condition-express] [-n <排名数量>] [--skipJDKMethod <是否跳过JDK方法>] [listener]
   ```

   其中，`class-pattern`和`method-pattern`用于指定你想要跟踪的类和方法名的模式匹配表达式。例如，如果你想监控所有`com.example.service`包下以`Service`结尾的类中的所有方法耗时，可以这样操作：

   ```shell
   trace com.example.service.*Service *.*
   ```

   这个命令会显示匹配到的方法每次被调用的耗时以及调用栈信息。

3. **分析结果**  
   执行上述命令后，Arthas会开始实时监控指定方法的调用情况，展示每个调用的详细耗时及调用路径。这将帮助你识别出哪些方法是最耗时的，从而进一步分析和优化。

**注意点解释：**

- **自动高亮显示**：Arthas的`trace`命令通过颜色编码直观地展示了方法调用的时间消耗，红色表示较慢的调用，这使得性能瓶颈一目了然。
- **自定义条件与排序**：通过添加条件表达式和调整输出的排名数量，你可以更加精确地控制监控范围，专注于最关键的信息。
- **结果查看方式**：虽然原始知识中提到了通过浏览器查看profiler结果的方式，但那部分是关于查看arthas-output下的结果，与直接监控方法耗时的`trace`命令不同。对于`trace`命令，结果会直接在命令行界面输出。

综上所述，利用Arthas的`trace`命令是监控Java应用中方法调用耗时的有效手段，可以帮助开发者快速定位和优化性能问题。请根据你的具体需求调整命令参数，以达到最佳的监控效果。

<font color="#949494">---------------</font>

## 参考链接 ：

[Arthas3.0 的新特性](../arthas3.md)

[profiler](../profiler.md)

[专家经验：利用Arthas排查NoSuchMethodError](./user-question-history14763.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16046)给我们反馈。
