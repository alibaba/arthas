# Arthas MCP stop 卡住问题分析

日期：2026-05-18
执行者：Codex

## 现象

用户提供的两次 jstack 结果分别为 `/tmp/aa.txt` 和 `/tmp/bb.txt`。两次 dump 间隔约 505 秒，但 Arthas 的 `stop` 命令执行线程一直停在同一个等待点。

关键线程：

```text
"arthas-command-execute" #38602 daemon ... nid=250680 waiting on condition
  - parking to wait for <0x00000006d7eaaec8> (a java.util.concurrent.CompletableFuture$Signaller)
  at java.util.concurrent.CompletableFuture.get(...)
  at com.taobao.arthas.core.mcp.ArthasMcpServer.stop(ArthasMcpServer.java:194)
  at com.taobao.arthas.core.mcp.ArthasMcpBootstrap.shutdown(ArthasMcpBootstrap.java:60)
  at com.taobao.arthas.core.server.ArthasBootstrap.destroy(ArthasBootstrap.java:533)
  at com.taobao.arthas.core.command.basic1000.StopCommand.shutdown(StopCommand.java:39)
```

## 证据

- `/tmp/aa.txt` 采集时间：2026-05-18 18:09:32。
- `/tmp/bb.txt` 采集时间：2026-05-18 18:17:57。
- 两次都是同一个 `arthas-command-execute` 线程、同一个 `tid`、同一个 `nid`、同一个 `CompletableFuture$Signaller` 对象。
- 两次 dump 中都没有 `java.lang.Thread.State: BLOCKED`。
- 两次 dump 中都没有 JVM 自动输出的 `Found one Java-level deadlock`。
- `mcp-keep-alive-scheduler` 线程仍然存在，说明 MCP streamable/keep-alive 相关组件仍处于生命周期内。

## 判断

这不是普通 Java monitor 死锁，也不是终端输出锁交错导致的 `TermResultDistributorImpl` / `ProcessImpl` 死锁。当前问题是 Arthas `stop` 流程进入 MCP server 关闭逻辑后，对 `closeGracefully()` 返回的 `CompletableFuture` 做了无超时 `get()`，当该 Future 没有完成时，`stop` 命令线程会无限等待。

## 根因

`ArthasMcpServer.stop()` 中存在无界等待：

```java
unifiedMcpHandler.closeGracefully().get();
streamableServer.closeGracefully().get();
statelessServer.closeGracefully().get();
```

这些调用位于 Arthas 销毁链路上：

```text
StopCommand.process
  -> ArthasBootstrap.destroy
  -> ArthasMcpBootstrap.shutdown
  -> ArthasMcpServer.stop
```

因此，只要 MCP handler/server 的关闭 Future 因异常路径、未完成会话、底层异步执行未调度等原因不完成，整个 `stop` 命令就无法返回，终端表现为卡住。

## 修复策略

- `ArthasMcpServer.stop()` 不允许无限等待单个 MCP 组件优雅关闭。
- 对每个 `closeGracefully()` Future 使用有限超时。
- 某个组件超时或失败时记录日志，并继续关闭后续组件与 task executor。
- 保留现有关闭顺序，避免扩大改动面。

## 验证计划

1. 构造一个 `closeGracefully()` 永不完成的 MCP handler，复现旧实现中 `stop()` 无法及时返回。
2. 增加回归测试，要求 `stop()` 在超时后返回。
3. 修改 `ArthasMcpServer.stop()` 为有限等待。
4. 执行新增测试和相关模块测试。

## 本地复现

新增测试 `core/src/test/java/com/taobao/arthas/core/mcp/ArthasMcpServerTest.java`，通过 Mockito 构造 `McpHttpRequestHandler.closeGracefully()` 返回一个永不完成的 `CompletableFuture`。

修复前执行：

```bash
./mvnw -pl core -Dtest=com.taobao.arthas.core.mcp.ArthasMcpServerTest#stopShouldNotWaitForeverWhenUnifiedHandlerCloseNeverCompletes test
```

结果：失败，`stopFuture.get(1500, TimeUnit.MILLISECONDS)` 超时，报错 `stop should return after MCP graceful shutdown timeout`。这与 jstack 中 `arthas-command-execute` 长时间停在 `CompletableFuture.get()` 的现象一致。

## 修复实现

修改 `core/src/main/java/com/taobao/arthas/core/mcp/ArthasMcpServer.java`：

- 每个 MCP 组件的 `closeGracefully()` 独立使用有限等待，当前超时为 5 秒。
- 单个组件超时、异常或中断时记录日志，并继续关闭后续组件。
- `taskExecutor` 保留 5 秒关闭等待；中断或异常时执行 `shutdownNow()`。
- 删除原先包住整个 `stop()` 的大 `try`，避免前一个组件关闭失败导致后续清理被跳过。

## 验证结果

修复后执行：

```bash
./mvnw -pl core -Dtest=com.taobao.arthas.core.mcp.ArthasMcpServerTest#stopShouldNotWaitForeverWhenUnifiedHandlerCloseNeverCompletes test
```

结果：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

继续执行相关测试：

```bash
./mvnw -pl core -Dtest=com.taobao.arthas.core.mcp.ArthasMcpServerTest,com.taobao.arthas.core.shell.system.impl.ProcessImplConcurrencyTest test
```

结果：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。

测试过程中仍会输出既有 Logback test 配置 `ConsoleAppender` 类型不兼容告警，以及 JDK restricted/deprecated API 告警；这些告警没有导致测试失败，且与本次 MCP stop 卡住修复无关。
