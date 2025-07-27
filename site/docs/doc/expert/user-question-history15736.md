---
id: "question-history-15736"
title: "arthas 中jad参数"
date: "2024-07-04"
category: "expertConsultation"
description: "在Arthas中，`jad`命令用于反编译已加载到JVM中的类的字节码为Java源代码，以便于理解类的内部实现。该命令提供了多种参数以适应不同的使用场景。针对您的问题，这里详细解析`jad`命令的各种参数及其用途：1. **基础用法与参数说明**：   - `_class-pattern_`：这是必"
---

## 用户问题 ：

arthas中jad参数

---

## 专家官方解答 ：

在Arthas中，`jad`命令用于反编译已加载到JVM中的类的字节码为Java源代码，以便于理解类的内部实现。该命令提供了多种参数以适应不同的使用场景。针对您的问题，这里详细解析`jad`命令的各种参数及其用途：

1. **基础用法与参数说明**：
   - `_class-pattern_`：这是必填参数，用于指定要反编译的类名，支持通配符匹配。
   - `[c:]`：可选参数，用于指定类加载器的哈希码，当存在多个类加载器加载了相同名称的类时非常有用。
   - `[classLoaderClass:]`：另一个可选参数，允许通过指定类加载器的类名来选择特定的类加载器，前提是此参数能唯一匹配到一个类加载器实例。
   - `[E]`：启用正则表达式匹配类名，而非默认的通配符匹配。

2. **高级功能与参数**：
   - `--source-only`：此选项让`jad`命令仅输出反编译得到的源代码，不包含类加载器信息等额外元数据。
   - `--lineNumber [true|false]`：控制是否在输出的源代码中包含行号，默认为`true`。设置为`false`则不显示行号。
   - `-d` 或 `--directory`: 指定反编译生成的class文件存放的目录，默认会根据logback.xml配置的log目录存放。

3. **示例应用**：
   - **反编译整个类**：如`jad java.lang.String`反编译String类。
   - **指定函数反编译**：如`jad demo.MathGame main`仅反编译MathGame类的main方法。
   - **处理多ClassLoader情况**：当一个类被多个ClassLoader加载时，使用`jad -c <hashcode> ClassName`指定具体ClassLoader的哈希码来反编译。

基于以上信息，如果您想使用`jad`命令进行特定操作或遇到具体问题，请提供更详细的需求或描述，以便提供更精确的指导。例如，如果您想要反编译特定类的某个方法并保存反编译结果到指定目录，您可以这样操作：

```shell
jad --source-only demo.MyClass myMethod -d /path/to/save/directory
```

这将只显示`demo.MyClass`中`myMethod`方法的源代码，并且不会包含行号信息，同时将反编译过程中产生的临时class文件保存到`/path/to/save/directory`。

<font color="#949494">---------------</font>

## 参考链接 ：

[jad](../jad.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16093)给我们反馈。
