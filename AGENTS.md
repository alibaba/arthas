# AGENTS.md

本仓库是 Arthas，包含 JVM 诊断工具的 Java 多模块源码、Web 控制台和 VuePress 文档站。以下约定适用于整个仓库。

## 工作原则

- 从第一性原理出发：先明确要解决的问题、输入输出和必须保持的约束，再定位相关实现与测试。
- 选择能完整满足当前需求的最小实现。优先直接的控制流和已有能力；新增抽象、依赖或配置必须有具体收益。
- 修复根因，避免用多层兜底、静默吞错、伪造默认值或反复重试掩盖错误。兼容与恢复逻辑只服务于已知场景，并明确触发条件、失败结果和终止条件。
- 在输入和系统边界校验，内部依赖明确的契约，避免层层重复防御。
- 保持修改聚焦，沿用附近代码的风格。保留用户已有改动，不顺带重构、格式化或清理无关文件。

## 工程入口

- `core/`：诊断命令、字节码增强、会话与结果渲染；命令实现在 `core/src/main/java/com/taobao/arthas/core/command/`。
- `boot/`、`agent/`、`spy/`：启动与 attach、类加载隔离、插桩回调。
- `arthas-mcp-server/`：MCP 接口；`tunnel-client/`、`tunnel-server/`：远程连接。
- `web-ui/arthasWebConsole/`：Vue Web 控制台。
- `site/docs/doc/`、`site/docs/en/doc/`：中英文用户文档；`site/docs/.vuepress/`：站点配置、导航、主题和静态资源。
- 各模块的 `src/test/` 放单元测试；`arthas-mcp-integration-test/`、`arthas-external-command-integration-test/` 和 `integration-test/` 放集成测试。

## 必须保持的约束

- Java 默认兼容 JDK 8，模块覆盖设置以对应 `pom.xml` 为准。JDK 17+ 构建会额外启用 Tunnel Server、MCP 和外部命令集成测试模块；构建行为以根 `pom.xml` 和 `.github/workflows/test.yaml` 为准。
- Arthas 运行在目标 JVM 内。诊断异常不能影响业务线程；保留必要的异常隔离和类加载隔离，确保监听器、增强和线程等资源在结束或取消时正确释放。
- 命令沿用现有 Command → Model → View 分工，保持结果数据与终端渲染分离；修改输出时检查 HTTP/MCP 等调用方的兼容性。
- 命令行为、参数或输出变化时，同步相关测试和中英文文档。增删文档页面时检查导航与相对链接。
- 修改源码，不手工编辑 `target/`、`node_modules/`、VuePress 的 `.temp/`、`.cache/`、`dist/` 等生成内容。

## 验证与交付

按改动选择能证明行为正确的最小验证范围；修复缺陷时优先补能复现问题的回归测试。跨模块或打包变更再扩大验证范围。

以下命令在仓库根目录执行：

```bash
# Java 模块测试：将 core 替换为受影响的模块
./mvnw -pl core -am test

# MCP 集成测试：需要 JDK 17+ 和 bash，verify 阶段才执行集成测试
./mvnw -pl arthas-mcp-integration-test -am verify

# 完整构建与测试
./mvnw clean install -P full

# 文档站构建：先按 site/README.md 安装依赖
npm --prefix site run docs:build
```

- 文档改动检查示例、链接和页面构建；纯说明文字调整无需运行全量 Java 测试。
- 提交前检查 `git diff --check` 和变更范围。只格式化修改涉及的文件，避免全仓库格式化。
- 交付时简要说明改了什么、验证结果及未验证项。环境或依赖阻塞时报告具体原因，不通过增加兜底或跳过检查来伪装成功。
