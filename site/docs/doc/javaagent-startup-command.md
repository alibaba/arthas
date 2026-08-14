# Java Agent 启动命令机制设计

日期：2026-07-23
执行者：Codex

## 背景

Arthas 已经支持通过 `-javaagent` 在应用启动前加载，也已经支持增强命令的
`--lazy` 模式。`--lazy` 会保留对应的 Transformer，在目标类首次加载时完成增强。

当前缺少的是启动阶段的命令编排：没有配置入口自动提交命令；一个 Arthas session
又只能有一个前台 job，因此不能把多个长期运行的 `watch`、`line` 命令依次放进同一
session；启动命令也没有独立、持久、可区分的结果输出。

## 目标

1. JVM 使用 `-javaagent` 启动 Arthas 时，可以从脚本自动执行指定命令。
2. `watch`、`line` 等增强命令在目标类尚未加载时自动等待，并在类首次加载时生效。
3. 多条长期运行命令使用独立 session/job，同时存活，互不阻塞。
4. `premain` 返回前确认启动命令已经经过 Arthas 命令执行线程，避免应用类加载与
   Transformer 注册之间的竞态。
5. 每条命令的结构化结果、文本输出、退出状态和丢弃统计持续写入独立文件。
6. 启动脚本或单条命令失败不阻断业务 JVM 启动，并留下可诊断结果。

## 非目标

- 不新增一套命令解析器，仍使用 Arthas 的 `CliTokens` 和 `JobController`。
- 不改变普通交互 session 的 lazy 默认值。
- 不保证无限速写盘。结果生产速度超过磁盘消费速度时，优先保护业务线程，并显式
  记录丢弃数量。
- 不提供启动命令的在线增删改 API；启动后仍可使用普通 Arthas 命令管理诊断任务。
- 首版不支持脚本续行、变量替换或 shell 控制语法。

## 配置与脚本契约

新增配置项：

```properties
arthas.startupScript=/opt/arthas/startup.as
```

相对路径以目标 JVM 的工作目录为基准。脚本使用 UTF-8，一行一条 Arthas 命令；空行
和去除前导空格后以 `#` 开头的行会被忽略。首版最多加载 32 条命令，单行最多 8192
个字符。

示例：

```text
# /opt/arthas/startup.as
watch com.example.OrderService createOrder '{params, returnObj}' -n 20
line --class com.example.OrderService --method createOrder --line 128 --express '{params, localVarMap}' -n 20
line --class com.example.InventoryService --line 76,81 --express '{params, localVarMap}' -n 50
```

推荐把配置写入 Arthas 目录中的 `arthas.properties`，然后正常启动：

```bash
java -javaagent:/opt/arthas/arthas-agent.jar -jar app.jar
```

也可以直接传递 agent options。由于现有 agent options 使用分号编码，完整 JVM 参数应
整体加引号，并保留开头和结尾的分号：

```bash
java '-javaagent:/opt/arthas/arthas-agent.jar=;startupScript=/opt/arthas/startup.as;' -jar app.jar
```

## 启动时序

```text
AgentBootstrap.premain
  -> ArthasBootstrap 初始化配置、命令注册和服务
  -> 初始化命令执行线程与 TransformerManager
  -> 读取 startupScript
  -> 为每条命令创建独立 session、结果文件和 job
  -> 把所有 job 提交到 Arthas 单线程命令执行器
  -> 在同一执行器尾部提交 barrier 并等待
  -> barrier 完成，premain 返回，应用 main 开始
```

job 的命令处理函数仍然按 Arthas 现有规则串行完成初始化，但 `watch`、`line` 等增强
命令安装完 listener/Transformer 后会立即从处理函数返回，job 本身继续运行。因此
barrier 完成时，多条长期命令已经同时处于监听状态。

barrier 最多等待 30 秒。超时会写入所有尚在运行的命令结果文件并记录 Arthas 日志，
随后允许 JVM 继续启动，避免诊断配置拖垮业务启动。

## 类尚未加载时的行为

启动命令 session 带有内部标记 `Session.STARTUP_COMMAND`。`EnhancerCommand` 的有效
lazy 值为：

```text
命令显式 --lazy || 当前 session 是启动命令 session
```

因此启动脚本里的 `watch`、`line`、`trace`、`stack` 等 `EnhancerCommand` 子类默认
具备 lazy 语义；普通终端、HTTP API 和 MCP session 的行为不变。

`Enhancer` 先处理已经加载的匹配类，再注册到 `TransformerManager` 的 lazy
Transformer 列表。后续类首次定义时，按类名、排除规则、ClassLoader 和 unsafe 规则
重新匹配并增强。多条命令拥有独立 listener/Transformer；同一个类加载事件会依次经过
所有匹配的 Transformer，所以多个 `line` 可以命中不同或相同的行号。

## session 与 job 模型

每条命令创建一个内部 session，原因是一个 session 同时只能持有一个前台 job。每个
session：

- 只运行脚本中的一条命令；
- 标记为 quiet 和 startup command；
- 使用独立结果分发器和输出文件；
- 在命令结束时自动移除；
- Arthas 停止时由启动命令管理器统一终止和关闭。

启动脚本来自本机 JVM 启动配置，属于可信的进程级配置。即使 Arthas 网络入口启用了
认证，内部启动 session 也可以执行；网络 session 的认证规则不变。`disabledCommands`
仍然生效，因为被禁用命令不会注册到命令管理器。

## 输出目录与格式

复用现有 `arthas.outputPath`，默认输出到：

```text
arthas-output/startup/<pid>/<run-id>/
```

目录结构：

```text
arthas-output/startup/12345/
├── latest
└── 20260723-153012-123/
    ├── manifest.json
    ├── command-001.jsonl
    ├── command-002.jsonl
    └── command-003.jsonl
```

`latest` 是 UTF-8 文本文件，内容为最新 `run-id`，避免依赖不兼容 Windows 的符号链接。
`manifest.json` 保存脚本绝对路径、PID、启动时间、命令序号、完整命令和结果文件名。

每个 JSONL 事件都包含 `sequence`、`timestamp`、`commandIndex`、`command` 和 `event`。
主要事件包括：

- `commandSubmitted`：job 提交前；
- `commandStarted`：job 已创建，包含 `jobId`；
- `result`：原始 `ResultModel`，保留 `watch`、`line` 等结构化字段；
- `stdout`：命令通过 `process.write` 产生的文本；
- `startupReady`：启动 barrier 已完成；
- `commandCompleted` / `commandFailed`：命令结束或创建失败；
- `resultsDropped`：异步写盘队列溢出，包含从上次记录起丢弃的事件数。

示例：

```json
{
  "sequence": 4,
  "timestamp": "2026-07-23 15:30:14.201",
  "commandIndex": 2,
  "command": "line --class com.example.OrderService --line 128",
  "event": "result",
  "resultType": "line",
  "result": {
    "type": "line",
    "className": "com.example.OrderService",
    "methodName": "createOrder",
    "lineNumber": 128
  }
}
```

结果分发使用每命令一个守护写线程和容量为 1024 的内存队列。业务线程只做非阻塞入队；
队列满时淘汰最早的待写事件并累计 `resultsDropped`，不在业务线程同步写磁盘。JSONL
每个事件写完即 flush，使早期启动故障后仍尽可能保留完整数据。

## 错误与退出策略

- 脚本不存在、不可读、命令数或行长超限：不执行任何启动命令，在 Arthas 日志记录
  错误，业务 JVM 继续启动。
- 单条命令不存在或参数错误：只结束该命令，在对应 JSONL 写入失败/状态事件，其他
  命令继续运行。
- 单条结果无法 JSON 序列化：写入 `serializationError` 事件，写线程继续工作。
- 输出目录或文件无法创建：不执行对应启动命令，避免产生无处可查的诊断任务。
- Arthas stop/JVM shutdown：先终止启动 jobs、注销 Transformer/listener，再排空并关闭
  结果文件。

## 兼容性

- 未显式传入 core JAR 路径时，agent 会把空值视为“未配置”，并从
  `arthas-agent.jar` 同目录查找 `arthas-core.jar`，保证最简 `-javaagent` 用法可用。
- 未配置 `arthas.startupScript` 时不创建额外 session、线程或文件，原行为不变。
- 普通命令只有显式指定 `--lazy/-L` 才使用 lazy，原行为不变。
- 复用 `arthas.outputPath`，不增加第二套输出根目录配置。
- 脚本复用现有命令语法；显式 `--lazy`、`--timeout`、`-n` 等选项继续有效。

## 验证计划

1. 配置绑定测试：`arthas.startupScript` 能注入 `Configure`。
2. 脚本解析测试：注释、空行、UTF-8 BOM、多命令、数量和行长限制。
3. 输出测试：manifest、result/stdout/lifecycle JSONL、正常关闭与序列化错误。
4. 编排测试：多条命令创建不同 session/job，单条失败不影响其他命令，barrier 可等待。
5. lazy 测试：启动 session 自动 lazy，普通 session 保持原默认。
6. 模块测试与编译：运行 core 相关单测并完成 Maven 编译验证。
