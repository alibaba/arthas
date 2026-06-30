---
name: arthas-springcontext-issues-resolve
description: 排查 Spring ApplicationContext / Bean / 配置注入等问题
---

# Spring Context / Bean 排查指南

原则：
- **先只读查询**（contains/beanNames/type/environment），避免直接 `getBean()` 触发 Bean 初始化产生副作用。
- **严格限量**：`vmtool -l` 控制实例数量；避免无条件输出完整 `getBeanDefinitionNames()`。


## 1) 获取并挑选正确的 ApplicationContext

优先尝试获取常见的 Spring Boot Context（通常是 `AbstractApplicationContext` 子类）：

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 5
```

如果拿不到结果，可以尝试获取： org.springframework.context.ApplicationContext

如果获取到多个对象，可以从对象的 classloader 的 Class<?> name 来判断。

1. 应用的 ClassLoader 通常是包含 `LaunchedURLClassLoader`
2. 应用的 ClassLoader 绝不会是 com.taobao.pandora.service.loader.ModuleClassLoader

## 2) 获取配置项的值与来源

只看值（示例：`server.port`）：

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].getEnvironment().getProperty("server.port")'
```

获取“来源”

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express '#env=instances[0].getEnvironment(), #ps=#env.getPropertySources().get("configurationProperties"), #ps.findConfigurationProperty("server.port")'
```

如果应用有集成 spring-boot-starter-actuator ，可以尝试

```bash
vmtool --action getInstances \
--className org.springframework.boot.actuate.env.EnvironmentEndpoint \
--express 'instances[0].environmentEntry("server.port")'
```

## 3) 按 Bean Name 验证是否存在（不触发初始化）

假设你要查的 beanName 为 `fooService`：

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].containsBean("fooService")'
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].containsLocalBean("fooService")'
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].containsBeanDefinition("fooService")'
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].getAliases("fooService")'
```

判读：
- `containsBean=true` 但 `containsLocalBean=false`：Bean 可能来自**父 Context**。
- `containsBean=false` 且你确定应该存在：优先检查**是否选错 Context**、`@Profile/@Conditional`、配置项/环境变量是否生效。

## 4) 在 Spring Context 里“搜” Bean（按关键词过滤，限制输出）

当你只有一个关键词（比如 `order` / `datasource`）而不知道精确 beanName 时：

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express '#ctx=instances[0], #names=@java.util.Arrays@asList(#ctx.getBeanDefinitionNames()), #m=#names.{? #this.toLowerCase().contains("order")}, #m.subList(0, @java.lang.Math@min(#m.size(), 50))'
```

说明：
- 先用关键词把范围收敛，再最多输出 50 个候选；拿到候选 beanName 后回到第 3 步逐个验证。

## 5) 按类型查找 Bean（最适合定位“注入到了哪个实现”）

当你知道目标类型（接口/父类）全限定名时（示例：`com.foo.OrderService`）：

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].getBeanNamesForType(@com.foo.OrderService@class)'
```

若返回多个候选（`NoUniqueBeanDefinitionException` 常见根因），只看候选名称即可：

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express 'instances[0].getBeansOfType(@com.foo.OrderService@class).keySet()'
```

提示：
- 若怀疑代理导致类型不匹配（JDK Proxy / CGLIB），应优先按**接口类型**查询，再决定是否需要获取实例进一步确认。
- 若 `@com.foo.OrderService@class` 报 `ClassNotFound`，通常是**类加载器不对**：先用 `classloader`（stats/instances/tree）找到应用的 `classLoaderHash`，再在 `vmtool/ognl` 上加 `--classLoader <hash>` 重新执行。也可以找到 `classloader`的 Class Name，再使用 `--classLoaderClass` 参数。

## 6) 查看 BeanDefinition（来源/工厂方法/作用域）

当你需要确认 Bean 是怎么注册进来的（`@Bean` 工厂方法？XML？自动扫描？）时，可尝试拿 `BeanFactory` 看 `BeanDefinition`（前提：Context 是 `AbstractApplicationContext`）。

```bash
vmtool --action getInstances --className org.springframework.context.support.AbstractApplicationContext -l 1 --express '#ctx=instances[0], #bf=#ctx.getBeanFactory(), #bd=#bf.getBeanDefinition("fooService")'
```