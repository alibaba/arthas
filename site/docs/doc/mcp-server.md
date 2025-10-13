# Arthas MCP Server

## 概览

Arthas MCP Server 是 Arthas 的实验性模块，实现了基于 [MCP（Model Context Protocol）](https://modelcontextprotocol.io/) 协议的服务端。该模块通过 HTTP/Netty 提供统一的 JSON-RPC 2.0 接口，使 AI 助手能够通过工具调用的方式执行 Arthas 诊断命令。

MCP（Model Context Protocol）是由 Anthropic 提出的一种标准化协议，用于连接 AI 助手与各种工具和数据源。通过 Arthas MCP Server，AI 助手可以自然地执行 Java 应用诊断任务，大幅提升开发和运维效率。

### 主要特性

- **AI 原生集成**：支持主流 AI 客户端（Claude Desktop、Cherry Studio、Cline 等）
- **标准化协议**：完整实现 MCP 协议规范（版本 2025-03-26），支持 Streamable Http 传输
- **26 个诊断工具**：涵盖 JVM 监控、类加载、方法追踪等核心功能
- **安全认证**：支持 Bearer Token 认证机制

## 支持的诊断工具

Arthas MCP Server 集成了 26 个核心诊断工具，按功能分类如下：

### JVM 相关工具（12 个）

| 工具            | 功能描述                                                  |
| --------------- | --------------------------------------------------------- |
| **dashboard**   | 实时展示 JVM/应用面板，支持自定义刷新间隔和次数控制       |
| **heapdump**    | 生成 JVM heap dump 文件，支持 `--live` 选项只导出存活对象 |
| **jvm**         | 查看当前 JVM 的详细信息                                   |
| **mbean**       | 查看或监控 MBean 属性信息，支持实时刷新和模式匹配         |
| **memory**      | 查看 JVM 的内存信息                                       |
| **thread**      | 查看线程信息及堆栈，支持查找阻塞线程和最忙线程            |
| **sysprop**     | 查看或修改系统属性，支持动态修改 JVM 系统属性             |
| **sysenv**      | 查看系统环境变量                                          |
| **vmoption**    | 查看或更新 VM 选项，支持动态调整 JVM 参数                 |
| **perfcounter** | 查看 Perf Counter 信息，显示 JVM 性能计数器               |
| **vmtool**      | 虚拟机工具集合，支持强制 GC、获取实例、线程中断等         |
| **getstatic**   | 查看类的静态字段值                                        |
| **ognl**        | 执行 OGNL 表达式，动态调用方法和访问字段                  |

### Class/ClassLoader 相关工具（8 个）

| 工具            | 功能描述                                             |
| --------------- | ---------------------------------------------------- |
| **sc**          | 查看 JVM 已加载的类信息，支持详细信息和统计          |
| **sm**          | 查看已加载类的方法信息，显示方法签名和修饰符         |
| **jad**         | 反编译指定已加载类的源码，将字节码反编译为 Java 代码 |
| **classloader** | ClassLoader 诊断工具，查看类加载器统计、继承树、URLs |
| **mc**          | 内存编译器，将 Java 源码编译为字节码文件             |
| **redefine**    | 重定义类，加载外部 class 文件重新定义 JVM 中的类     |
| **retransform** | 重新转换类，触发类的重新转换和字节码增强             |
| **dump**        | 将 JVM 中实际运行的 class 字节码导出到指定目录       |

### 监控诊断工具（6 个）

| 工具        | 功能描述                                                       |
| ----------- | -------------------------------------------------------------- |
| **monitor** | 实时监控指定类的指定方法的调用情况                             |
| **stack**   | 输出当前方法被调用的调用路径，帮助分析方法的调用链路           |
| **trace**   | 追踪方法内部调用路径，输出每个节点的耗时信息                   |
| **tt**      | 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息 |
| **watch**   | 观察指定方法的调用情况，包含入参、返回值和抛出异常等信息       |

## 快速开始

### 1. 配置 MCP 服务

在 `arthas.properties` 配置文件中启用 MCP 服务：

```properties
# MCP (Model Context Protocol) configuration
arthas.mcpEndpoint=/mcp
```

### 2. 启动应用

正常启动 Arthas 或带有 Arthas 的 Java 应用。默认情况下，MCP 服务会在 HTTP 端口 8563 上暴露。

验证 MCP 服务是否启动：

```bash
curl http://localhost:8563/mcp
```

如果返回 MCP 协议信息，说明服务已成功启动。

### 3. 配置 AI 客户端

以下是几种主流 AI 客户端的配置方式：

#### Cherry Studio / Cline

在设置中添加 MCP 服务器配置：

```json
{
  "mcpServers": {
    "arthas-mcp": {
      "type": "streamableHttp",
      "url": "http://localhost:8563/mcp"
    }
  }
}
```

## 配置说明

### Arthas 配置项

| 配置项               | 说明                       | 默认值             |
| -------------------- | -------------------------- | ------------------ |
| `arthas.mcpEndpoint` | MCP 服务的访问路径         | 无（需要手动配置） |
| `arthas.httpPort`    | HTTP 服务端口              | 8563               |
| `arthas.password`    | 认证密码（开启认证时使用） | 无                 |

### 认证配置

当在配置文件中设置了 `arthas.password` 时，MCP Server 会自动开启鉴权功能。此时需要在 AI 客户端配置中添加认证头，携带的 Bearer Token 就是配置的密码值。

配置文件示例（`arthas.properties`）：

```properties
arthas.password=your-secure-password
```

AI 客户端配置示例：

```json
{
  "mcpServers": {
    "arthas-mcp": {
      "type": "streamableHttp",
      "url": "http://localhost:8563/mcp",
      "headers": {
        "Authorization": "Bearer your-secure-password"
      }
    }
  }
}
```

::: warning
**注意**：Authorization header 中的 token 必须与 `arthas.password` 配置的值完全一致。
:::

## 反馈与贡献

::: tip
Arthas MCP Server 是实验性功能，欢迎提供反馈和建议！
:::

- **问题反馈**：[GitHub Issues](https://github.com/alibaba/arthas/issues)
- **功能建议**：[GitHub Discussions](https://github.com/alibaba/arthas/discussions)
- **参与贡献**：[贡献指南](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
