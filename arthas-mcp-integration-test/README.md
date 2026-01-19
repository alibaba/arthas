# arthas-mcp-integration-test

本模块提供 Arthas MCP Server 的集成测试：

- 测试会启动一个独立的目标 JVM（`TargetJvmApp`）。
- 通过 `packaging/target/arthas-bin/as.sh` 动态 attach 到目标 JVM，在目标 JVM 内启动 Arthas Server（仅开启 HTTP 端口，telnet 端口设置为 0）。
- 使用最小 MCP（Streamable HTTP + SSE）客户端调用 `tools/list` 与 `tools/call`，验证 MCP tools 功能可用。

## 运行方式

在项目根目录执行：

```bash
./mvnw -pl arthas-mcp-integration-test -am verify
```

说明：

- `-am` 会确保 `packaging` 等依赖模块先构建，从而在 `packaging/target/arthas-bin` 生成可用的 `as.sh` 与相关 jar。
- 该集成测试依赖本机 `bash`，Windows 环境会自动跳过。

