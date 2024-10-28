---
id: "question-history-11442"
title: "arthas 介绍一下"
date: "2024-07-04"
category: "expertConsultation"
description: "Arthas 是由阿里巴巴开源的一款强大的Java诊断工具，它能够帮助开发者在不修改代码、不重启服务的情况下，对线上问题进行快速诊断和定位。Arthas的功能覆盖了类加载信息查询、代码热更新、方法调用追踪、性能瓶颈监控、JVM实时状态监控等多种场景，极大地提高了问题排查的效率。### Arthas能"
---

## 用户问题 ：

arthas 介绍一下

---

## 专家官方解答 ：

Arthas 是由阿里巴巴开源的一款强大的Java诊断工具，它能够帮助开发者在不修改代码、不重启服务的情况下，对线上问题进行快速诊断和定位。Arthas的功能覆盖了类加载信息查询、代码热更新、方法调用追踪、性能瓶颈监控、JVM实时状态监控等多种场景，极大地提高了问题排查的效率。

### Arthas能做什么？

- **类加载问题定位**：告诉你类从哪个jar包加载，以及类加载异常的原因。
- **代码变更验证**：无需重启应用即可验证代码变更是否生效。
- **线上问题调试**：提供类比debug的体验，无需添加日志重新部署。
- **用户问题复现**：即使线下环境难以复现，也能在线上直接诊断特定用户的问题。
- **系统全局视图**：提供系统运行时的全局信息概览。
- **JVM状态监控**：实时查看JVM的内存、线程等关键指标。
- **性能热点分析**：快速定位并生成热点方法的火焰图。
- **实例对象搜索**：在JVM中直接查找指定类的实例对象。

### 如何快速启动Arthas？

#### 方法1：使用arthas-boot安装（推荐）

1. **下载arthas-boot.jar**
   ```bash
   curl -O https://arthas.aliyun.com/arthas-boot.jar
   ```
2. **选择目标Java进程并挂载Arthas**
   ```bash
   java -jar arthas-boot.jar
   ```
   在显示的进程列表中选择需要诊断的进程编号，如选择示例中的`math-game.jar`。

#### 方法2：下载arthas并手动启动

- 访问[Arthas GitHub Release页面](https://github.com/alibaba/arthas/releases)或[Maven仓库](https://arthas.aliyun.com/download/arthas-packaging/latest_version?mirror=aliyun)下载对应版本的arthas。
- 通过命令行进入arthas所在目录并启动它，例如：
  ```bash
  java -jar arthas.jar <目标Java进程PID>
  ```

### 成功安装验证

成功启动Arthas后，你会看到包含版本信息、PID以及一个简单的命令行界面。此时，你可以开始使用Arthas提供的各种命令进行诊断，如输入`dashboard`查看实时数据面板。

### Arthas Tunnel Server部署（可选）

对于需要远程诊断的场景，可以部署Arthas Tunnel Server：

1. **下载arthas-tunnel-server.jar**，从GitHub Releases或Maven仓库获取。
2. **启动Tunnel Server**
   ```bash
   java -jar arthas-tunnel-server.jar
   ```
3. **通过Web界面连接到Arthas Agent**，访问`http://127.0.0.1:8080/`，使用arthas隧道ID连接到远程机器上的Arthas代理。

通过上述步骤，你可以充分利用Arthas的强大功能来提升Java应用的诊断效率。

<font color="#949494">---------------</font>

## 参考链接 ：

[Arthas Tunnel](../tunnel.md)

[专家经验：Arthas 快速启动](./user-question-history11440.md)

<font color="#949494">---------------</font>

## <font color="#FF0000">答疑服务说明：</font>

本内容经由技术专家审阅的用户问答的镜像生成，我们提供了<font color="#FF0000">专家智能答疑服务</font>，在<font color="#FF0000">页面的右下的浮窗”专家答疑“</font>。您也可以访问 : [全局专家答疑](https://answer.opensource.alibaba.com/docs/intro) 。 咨询其他产品的的问题

### 反馈

如问答有错漏，欢迎点：[差评](https://ai.nacos.io/user/feedbackByEnhancerGradePOJOID?enhancerGradePOJOId=16041)给我们反馈。
