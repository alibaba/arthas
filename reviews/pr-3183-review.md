# PR #3183 Review Notes

Date: 2026-04-29
Reviewer: Codex

## Scope reviewed
- common/src/main/java/com/taobao/arthas/common/ArthasConstants.java
- tunnel-client/src/main/java/com/alibaba/arthas/tunnel/client/ProxyClient.java
- tunnel-client/src/test/java/com/taobao/arthas/common/ArthasConstantsTest.java
- tunnel-client/pom.xml

## Summary
The change is well-scoped and addresses the reported tunnel-client JFR download limit by making only the proxy HTTP aggregator limit configurable.

## Findings
No blocking issues found.

## 配置项使用说明（面向用户）
新增配置项是 JVM 系统属性（`-D` 参数），在启动 `tunnel-client` 时传入即可。

- 启动参数示例（以 PR 中新增常量对应的 key 为准）：
  - `java -D<NEW_PROXY_MAX_CONTENT_LENGTH_KEY>=67108864 -jar arthas-tunnel-client.jar`
- 单位是字节（bytes），`67108864` = `64MB`。
- 仅影响 tunnel-client 里 proxy HTTP 聚合器的最大内容长度，不影响其它模块默认值。
- 建议按实际 JFR 文件大小留有余量（例如 2~4 倍）。
