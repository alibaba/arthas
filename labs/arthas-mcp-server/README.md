# arthas-mcp-server

## 项目简介

`arthas-mcp-server` 是 [Arthas](https://github.com/alibaba/arthas) 的实验模块，实现了基于 MCP（Model Context Protocol）协议的服务端。该模块通过 HTTP/Netty 提供统一的 JSON-RPC 2.0 接口，支持 AI 使用工具调用的方式执行 arthas 的命令。

Arthas MCP 服务集成了 26 个核心诊断工具，按功能分类如下：

### JVM 相关工具

• **dashboard** - 实时展示 JVM/应用面板，支持自定义刷新间隔和次数控制

• **heapdump** - 生成 JVM heap dump 文件，支持 --live 选项只导出存活对象

• **jvm** - 查看当前 JVM 的信息

• **mbean** - 查看或监控 MBean 属性信息，支持实时刷新和模式匹配

• **memory** - 查看 JVM 的内存信息

• **thread** - 查看线程信息及堆栈，支持查找阻塞线程和最忙线程

• **sysprop** - 查看或修改系统属性，支持动态修改 JVM 系统属性

• **sysenv** - 查看系统环境变量

• **vmoption** - 查看或更新 VM 选项，支持动态调整 JVM 参数

• **perfcounter** - 查看 Perf Counter 信息，显示 JVM 性能计数器

• **vmtool** - 虚拟机工具集合，支持强制 GC、获取实例、线程中断等

• **getstatic** - 查看类的静态字段值

• **ognl** - 执行 OGNL 表达式，动态调用方法和访问字段

### Class/ClassLoader 相关工具

• **sc** - 查看 JVM 已加载的类信息，支持详细信息和统计

• **sm** - 查看已加载类的方法信息，显示方法签名和修饰符

• **jad** - 反编译指定已加载类的源码，将字节码反编译为 Java 代码

• **classloader** - ClassLoader 诊断工具，查看类加载器统计、继承树、URLs

• **mc** - 内存编译器，将 Java 源码编译为字节码文件

• **redefine** - 重定义类，加载外部 class 文件重新定义 JVM 中的类

• **retransform** - 重新转换类，触发类的重新转换和字节码增强

• **dump** - 将 JVM 中实际运行的 class 字节码导出到指定目录

### 监控诊断工具

• **monitor** - 实时监控指定类的指定方法的调用情况

• **stack** - 输出当前方法被调用的调用路径，帮助分析方法的调用链路

• **trace** - 追踪方法内部调用路径，输出每个节点的耗时信息，支持条件过滤和耗时阈值设置

• **tt** - 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，支持事后查看和重放

• **watch** - 观察指定方法的调用情况，包含入参、返回值和抛出异常等信息，支持实时流式输出


## 快速开始

首先需要在 arthas.properties 中配置 mcp 服务的 path：

```JSON
# MCP (Model Context Protocol) configuration
arthas.mcpEndpoint=/mcp
```

正常启动服务之后，服务对外暴露8563，在 cherry-studio/cline 等 ai 客户端中配置：

在设置中添加 MCP 服务器：

```JSON
{
  "mcpServers": {
    "arthas-mcp": {
      "type": "streamableHttp",
      "url": "http://localhost:8563/mcp"
    }
  }
}
```


开启认证服务的时候需要添加 headers，这里的 token 直接使用 password：

```java
"arthas-mcp-streamable-server": {
      "type": "streamableHttp",
      "url": "http://localhost:8563/mcp",
      "headers": {
        "Authorization": "Bearer password"
      }
    }
```