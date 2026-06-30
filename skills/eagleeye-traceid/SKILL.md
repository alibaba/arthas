---
name: arthas-eagleeye-traceid
description: 使用 Arthas 的 watch/trace 获取 EagleEye traceId / 获取请求的 traceId
---

# 获取 EagleEye traceId（Arthas）

适用场景：你需要在不改代码的情况下，在线上请求链路里拿到当前线程的 EagleEye `traceId`，用于关联日志/链路分析/问题复现。

核心思路：
- EagleEye 的 traceId 可在业务线程里通过 `EagleEye.getTraceId()` 获取。
- Arthas 的 `watch` 支持 OGNL，可直接调用静态方法：`@com.taobao.eagleeye.EagleEye@getTraceId()`
- Arthas 的 `trace` 输出中通常会自动包含 `trace_id=...`（如果环境集成了 EagleEye）。

## 前置检查（推荐）

1) 确认 EagleEye 类是否存在：

```bash
sc -d com.taobao.eagleeye.EagleEye
```

若找不到：
- 可能未集成 EagleEye、或类被 relocate/shade（请让用户提供实际类名/依赖信息）。

2) 选择一个“确定会被请求线程调用”的方法作为观察点：
- 常见：Controller 入口方法、RPC Provider 方法、Filter/Interceptor 的 doFilter/preHandle 等。
- 避免：高频热点方法（容易刷屏、增加开销）。

## 方案 A：用 watch 直接打印 traceId（最直观）

### A1) 只打印 traceId（最小输出）

```bash
watch <类全名> <方法名> '@com.taobao.eagleeye.EagleEye@getTraceId()' -n 5
```

说明：
- `@类名@静态方法()` 是 OGNL 静态方法调用语法。
- `-n 5` 限制执行次数，避免线上刷屏（务必保留/调整）。

### A2) 同时打印参数 + traceId（更好做关联）

```bash
watch <类全名> <方法名> '{params, @com.taobao.eagleeye.EagleEye@getTraceId()}' -n 5 -x 2
```

说明：
- `{...}` 会以数组/列表方式输出多个字段。
- `params` 是 Arthas watch 内置变量之一；`-x 2` 控制对象展开深度（可按需调大/调小）。

常见变体（按需）：
- 在方法返回后再取（默认就是 after，可不写）：
  - 如果你怀疑 traceId 在方法执行过程中才被设置，可尝试 `-b`（before）对比；具体以线上效果为准。

## 方案 B：用 trace 自动带出 traceId（更适合看调用链）

```bash
trace <类全名> <方法名> -n 5
```

期待现象：
- `trace` 输出的头部信息里出现类似 `trace_id=<xxxx>` 的字段。
- 同时你还能看到方法调用链与每段耗时，便于“拿 traceId + 看慢点在哪”一并完成。

## 结果解读与产出（建议模板）

最终给用户的结论/证据建议包含：
- 观察点（类/方法）：`<类全名>#<方法名>`
- 获取方式：`watch` / `trace`
- 关键证据：
  - `traceId=<...>`（以及必要时的 params 摘要）
- 下一步建议：
  - 用该 traceId 去日志/链路系统查询对应请求
  - 或将观察点收敛到更下游的方法（如某个 DAO/RPC 调用）继续 trace/watch

## 扩展

- 其他分布式追踪系统的 traceId、或 ThreadLocal 里的值，也可以用类似方式在 `watch` 的 OGNL 表达式中读取。

