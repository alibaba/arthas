# Arthas MCP Server

## Overview

Arthas MCP Server is an experimental module of Arthas that implements a server based on the [MCP (Model Context Protocol)](https://modelcontextprotocol.io/). This module provides a unified JSON-RPC 2.0 interface through HTTP/Netty, enabling AI assistants to execute Arthas diagnostic commands via tool calls.

MCP (Model Context Protocol) is a standardized protocol proposed by Anthropic for connecting AI assistants with various tools and data sources. Through Arthas MCP Server, AI assistants can naturally execute Java application diagnostic tasks, significantly improving development and operations efficiency.

### Key Features

- **AI-Native Integration**: Supports mainstream AI clients (Claude Desktop, Cherry Studio, Cline, etc.)
- **Standardized Protocol**: Full implementation of MCP protocol specification (version 2025-03-26), supporting Streamable Http transport
- **26 Diagnostic Tools**: Covers core functionalities including JVM monitoring, class loading, method tracing, etc.
- **Security Authentication**: Supports Bearer Token authentication mechanism

## Supported Diagnostic Tools

Arthas MCP Server integrates 26 core diagnostic tools, categorized by functionality:

### JVM-Related Tools (12)

| Tool | Description |
|------|-------------|
| **dashboard** | Real-time JVM/application dashboard with customizable refresh interval and count control |
| **heapdump** | Generate JVM heap dump file, supports `--live` option to export only live objects |
| **jvm** | View current JVM detailed information |
| **mbean** | View or monitor MBean attributes, supports real-time refresh and pattern matching |
| **memory** | View JVM memory information |
| **thread** | View thread information and stack traces, supports finding blocked and busiest threads |
| **sysprop** | View or modify system properties, supports dynamic JVM system property modification |
| **sysenv** | View system environment variables |
| **vmoption** | View or update VM options, supports dynamic JVM parameter adjustment |
| **perfcounter** | View Perf Counter information, displays JVM performance counters |
| **vmtool** | VM tool collection, supports forced GC, instance retrieval, thread interruption, etc. |
| **getstatic** | View static field values of a class |
| **ognl** | Execute OGNL expressions, dynamically invoke methods and access fields |

### Class/ClassLoader Tools (8)

| Tool | Description |
|------|-------------|
| **sc** | View loaded class information in JVM, supports detailed info and statistics |
| **sm** | View method information of loaded classes, displays method signatures and modifiers |
| **jad** | Decompile loaded class source code, converts bytecode to Java code |
| **classloader** | ClassLoader diagnostic tool, view classloader statistics, hierarchy tree, URLs |
| **mc** | Memory compiler, compiles Java source code to bytecode files |
| **redefine** | Redefine classes, load external class files to redefine classes in JVM |
| **retransform** | Retransform classes, triggers class retransformation and bytecode enhancement |
| **dump** | Dump actual running class bytecode from JVM to specified directory |

### Monitoring and Diagnostic Tools (6)

| Tool | Description |
|------|-------------|
| **monitor** | Monitor invocation of specified methods in real-time |
| **stack** | Output call path of current method, helps analyze method call chains |
| **trace** | Trace internal method call paths, output time cost for each node |
| **tt** | Time tunnel for method execution data, records parameters and return values for each invocation |
| **watch** | Observe method invocations including parameters, return values, and exceptions |

## Quick Start

### 1. Configure MCP Service

Enable MCP service in `arthas.properties` configuration file:

```properties
# MCP (Model Context Protocol) configuration
arthas.mcpEndpoint=/mcp
```

### 2. Start Application

Start Arthas or Java application with Arthas normally. By default, MCP service will be exposed on HTTP port 8563.

Verify MCP service is running:

```bash
curl http://localhost:8563/mcp
```

If MCP protocol information is returned, the service has started successfully.

### 3. Configure AI Client

Configuration examples for mainstream AI clients:

#### Cherry Studio / Cline

Add MCP server configuration in settings:

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

## Configuration

### Arthas Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `arthas.mcpEndpoint` | MCP service access path | None (requires manual configuration) |
| `arthas.httpPort` | HTTP service port | 8563 |
| `arthas.password` | Authentication password (when authentication enabled) | None |

### Authentication Configuration

When `arthas.password` is configured in the configuration file, MCP Server will automatically enable authentication. In this case, you need to add an authentication header in the AI client configuration, with the Bearer Token set to the configured password value.

Configuration file example (`arthas.properties`):

```properties
arthas.password=your-secure-password
```

AI client configuration example:

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
**Note**: The token in the Authorization header must exactly match the value configured in `arthas.password`.
:::

## Feedback & Contribution

::: tip
Arthas MCP Server is an experimental feature. Feedback and suggestions are welcome!
:::

- **Issue Report**: [GitHub Issues](https://github.com/alibaba/arthas/issues)
- **Feature Request**: [GitHub Discussions](https://github.com/alibaba/arthas/discussions)
- **Contribute**: [Contributing Guide](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
