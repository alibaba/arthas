# Arthas MCP Server

## Overview

Arthas MCP Server is an experimental module of Arthas that implements a server based on the [MCP (Model Context Protocol)](https://modelcontextprotocol.io/). This module provides a unified JSON-RPC 2.0 interface through HTTP/Netty, enabling AI assistants to execute Arthas diagnostic commands via tool calls.

MCP (Model Context Protocol) is a standardized protocol proposed by Anthropic for connecting AI assistants with various tools and data sources. Through Arthas MCP Server, AI assistants can naturally execute Java application diagnostic tasks, significantly improving development and operations efficiency.

### Key Features

- **AI-Native Integration**: Supports mainstream AI clients (Claude Desktop, Cherry Studio, Cline, etc.)
- **Standardized Protocol**: Full implementation of MCP protocol specification (version 2025-06-18), supporting Streamable Http transport
- **29 Diagnostic Tools**: Covers core functionalities including JVM monitoring, class loading, method tracing, etc.
- **Security Authentication**: Supports Bearer Token authentication mechanism

## Supported Diagnostic Tools

Arthas MCP Server integrates 29 diagnostic tools, categorized by functionality:

### JVM-Related Tools (13)

| Tool            | Description                                                                               |
| --------------- | ----------------------------------------------------------------------------------------- |
| **dashboard**   | Real-time JVM/application dashboard with customizable refresh interval and count control  |
| **heapdump**    | Generate JVM heap dump file, supports `--live` option to export only live objects         |
| **jvm**         | View current JVM detailed runtime information                                             |
| **mbean**       | View or monitor MBean attributes, supports real-time refresh and pattern matching         |
| **memory**      | View JVM memory usage                                                                     |
| **thread**      | View thread information and stack traces, supports finding blocked and busiest threads    |
| **sysprop**     | View or modify system properties, supports dynamic JVM system property modification       |
| **sysenv**      | View system environment variables                                                         |
| **vmoption**    | View or update VM options, supports dynamic JVM parameter adjustment                      |
| **perfcounter** | View JVM Perf Counter information                                                         |
| **vmtool**      | VM tool collection, supports forced GC, instance retrieval, thread interruption, etc.     |
| **getstatic**   | View static field values of a class, supports specifying ClassLoader and OGNL expressions |
| **ognl**        | Execute OGNL expressions, dynamically invoke methods and access fields                    |

### Class/ClassLoader Tools (8)

| Tool            | Description                                                                                                                                            |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **sc**          | Search loaded classes in JVM, supports wildcard and regex matching, view class details (classloader, interfaces, superclass, annotations, etc.)        |
| **sm**          | Search methods of loaded classes, supports wildcard and regex matching, view method signatures, parameter types, annotations, etc.                     |
| **jad**         | Decompile loaded class source code, converts actual running class bytecode in JVM to Java code                                                         |
| **classloader** | ClassLoader diagnostic tool, view classloader statistics, hierarchy tree, URLs, supports resource lookup and class loading; prefer sc for class search |
| **mc**          | Memory compiler, compiles `.java` source files to `.class` bytecode files                                                                              |
| **redefine**    | Reload class bytecode, allows hot-updating existing classes in the JVM at runtime                                                                      |
| **retransform** | Hot-load class bytecode, apply bytecode modifications to loaded classes and make them effective                                                        |
| **dump**        | Dump actual running class bytecode from JVM to specified directory, suitable for batch downloading bytecode of specified packages                      |

### Monitoring and Diagnostic Tools (6)

| Tool         | Description                                                                                                                     |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| **monitor**  | Monitor invocation of specified methods in real-time, outputs call count, success rate, average RT, and other statistics        |
| **stack**    | Output call path of current method, helps analyze method call chains                                                            |
| **trace**    | Trace internal method call paths, output time cost for each node, supports condition filtering                                  |
| **tt**       | Time tunnel for method execution data, records parameters and return values for each invocation, supports replay and inspection |
| **watch**    | Observe method invocations including parameters, return values, and exceptions, supports real-time streaming output             |
| **profiler** | Async Profiler diagnostic tool, samples CPU/alloc/lock events and outputs flamegraph, JFR, and other formats                    |

### Arthas Utility Tools (2)

| Tool         | Description                                                                                                                                     |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **viewfile** | View file contents (only within configured directory whitelist), supports cursor/offset pagination to avoid returning large content all at once |
| **options**  | View or modify Arthas global options                                                                                                            |

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

| Property             | Description                                                                 | Default                              |
| -------------------- | --------------------------------------------------------------------------- | ------------------------------------ |
| `arthas.mcpEndpoint` | MCP service access path                                                     | None (requires manual configuration) |
| `arthas.mcpProtocol` | Transport protocol mode: `STREAMABLE` (stateful) or `STATELESS` (stateless) | `STREAMABLE`                         |
| `arthas.httpPort`    | HTTP service port                                                           | 8563                                 |
| `arthas.password`    | Authentication password (when authentication enabled)                       | None                                 |

### Transport Protocol Mode

Arthas MCP Server supports two transport protocol modes:

- **STREAMABLE mode** (default): Stateful mode, maintains persistent connections via HTTP/SSE, supports long-running commands (e.g. watch, trace, monitor and other streaming tools), progress notifications, and session state. Suitable for interactive diagnostic scenarios.
- **STATELESS mode**: Stateless mode, each request is independent. Suitable for simple one-off query scenarios.

Configure in `arthas.properties`:

```properties
arthas.mcpEndpoint=/mcp
# Optional, defaults to STREAMABLE
arthas.mcpProtocol=STREAMABLE
```

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

### viewfile Directory Whitelist Configuration

The `viewfile` tool by default only allows viewing files in the following directories:

- The `arthas-output` directory under the current working directory (if it exists)
- The `~/logs/` directory under the user's home directory (if it exists)

Additional directories can be configured via environment variable:

```bash
export ARTHAS_MCP_VIEWFILE_ALLOWED_DIRS=/path/to/dir1,/path/to/dir2
```

## Feedback & Contribution

::: tip
Arthas MCP Server is an experimental feature. Feedback and suggestions are welcome!
:::

- **Issue Report**: [GitHub Issues](https://github.com/alibaba/arthas/issues)
- **Feature Request**: [GitHub Discussions](https://github.com/alibaba/arthas/discussions)
- **Contribute**: [Contributing Guide](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
