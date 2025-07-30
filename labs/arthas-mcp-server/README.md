# arthas-mcp-server

## 项目简介

`arthas-mcp-server` 是 [Arthas](https://github.com/alibaba/arthas) 的实验模块，实现了基于 MCP（Model Context Protocol）协议的服务端。该模块通过 HTTP/Netty 提供统一的 JSON-RPC 2.0 接口，支持 AI 使用工具调用的方式执行 arthas 的命令。

## 快速开始

正常启动服务之后，服务对外暴露8563，在 cherry-studio/cline 等 ai 客户端中配置：

在设置中添加 MCP 服务器：

```JSON
{
  "mcpServers": {
    "arthas-mcp": {
      "transport": "sse",
      "url": "http://localhost:8563/sse"
    }
  }
}
```
