---
name: arthas
description: arthas 诊断 java应用，jvm问题 skill
---

# Arthas 诊断 Skill

## Overview

Arthas 是 Java 应用在线诊断工具，本 Skill 包含多个子场景的诊断指南。使用时请根据用户描述的问题匹配对应场景，按指南中的步骤逐步排查。

**通用原则：**
- 先用低风险、只读的命令收集信息，再按需使用有侵入性的命令。
- 所有 `watch` / `trace` / `tt` / `stack` 等命令**必须**设置 `-n`（执行次数限制），避免对线上应用造成压力。
- 输出结论时附上关键证据（命令输出摘要），并给出明确的下一步建议。

---

## 子场景索引

### 1. CPU 飙高排查

**文件：** `cpu-high/SKILL.md`

适用场景：机器 CPU 飙高、应用响应变慢、负载异常升高。

核心步骤：
1. `dashboard` 查看 CPU / 线程 / GC 概况
2. `thread`（topN）定位最忙线程及堆栈
3. 根据堆栈判断方向（CPU 密集计算 / 锁竞争 / GC 等）
4. 按需用 `stack` / `trace` / `watch` 进一步确认热点方法调用路径
5. 输出诊断结论（现象、证据、初步结论、下一步建议）

---

### 2. 获取 EagleEye traceId

**文件：** `eagleeye-traceid/SKILL.md`

适用场景：需要在不改代码的情况下，获取线上请求的 EagleEye traceId，用于关联日志 / 链路分析。

核心步骤：
1. `sc -d com.taobao.eagleeye.EagleEye` 确认类存在
2. 选择合适的观察点（Controller / RPC Provider / Filter 等入口方法）
3. **方案 A**：`watch` + OGNL 表达式 `@com.taobao.eagleeye.EagleEye@getTraceId()` 直接打印 traceId
4. **方案 B**：`trace` 输出中自动带出 traceId，同时可看调用链耗时
5. 拿到 traceId 后去日志 / 链路系统查询对应请求

---

### 3. Spring Context / Bean 排查

**文件：** `spring-context/SKILL.md`

适用场景：排查 Spring ApplicationContext / Bean / 配置注入等问题。

核心步骤：
1. `vmtool --action getInstances` 获取 `AbstractApplicationContext` 实例（注意通过 ClassLoader 区分正确的 Context）
2. 获取配置项的值与来源（`getEnvironment().getProperty(...)` / `findConfigurationProperty(...)`）
3. `containsBean` / `containsLocalBean` / `containsBeanDefinition` 验证 Bean 是否存在（不触发初始化）
4. 按关键词过滤搜索 Bean（`getBeanDefinitionNames` + OGNL 过滤）
5. 按类型查找 Bean（`getBeanNamesForType` / `getBeansOfType`）
6. 查看 BeanDefinition 确认 Bean 注册来源（`@Bean` 工厂方法 / XML / 自动扫描）

**注意：** 先只读查询，避免直接 `getBean()` 触发 Bean 初始化产生副作用；若遇到 `ClassNotFound`，通常是类加载器不对，需先用 `classloader` 命令找到正确的 `classLoaderHash`。
