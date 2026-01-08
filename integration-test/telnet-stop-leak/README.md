# telnet-stop-leak 集成测试

目的：对同一目标 JVM 反复执行 `attach -> telnet 执行多命令 -> stop -> jmap`，检查 `com.taobao.arthas.agent.ArthasClassloader` 实例数是否随轮次增长。

## 依赖

- JDK（需要 `java`、`jmap`）
- `expect`、`telnet`
- Maven（用于构建 `packaging/target/arthas-bin`）

## 本地运行

1. 构建打包产物：

```bash
mvn -V -ntp -pl packaging -am package -DskipTests
```

2. 运行测试（默认会创建临时目录保存日志；建议指定 `--work-dir` 便于排查）：

`threshold` 可以考虑设置更高，JVM不能保证 ArthasClassLoader 必定会被回收。

```bash
python3 integration-test/telnet-stop-leak/run_telnet_stop_leak_test.py \
  --iterations 10 \
  --warmup 2 \
  --threshold 3 \
  --work-dir integration-test/telnet-stop-leak/work
```

输出目录里会生成：

- `results.csv`：每轮统计的 `ArthasClassloader` 实例数
- `logs/`：目标 JVM、attach、telnet transcript、jmap 错误输出

## 调整覆盖面

- 命令集合：`integration-test/telnet-stop-leak/commands.txt`
- telnet 执行逻辑：`integration-test/telnet-stop-leak/arthas_telnet.exp`

