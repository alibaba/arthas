# OGNL 复杂表达式使用技巧

本页介绍 Arthas 中较复杂、但仍适合在线诊断的 OGNL 写法，重点说明变量、集合筛选与投影、临时结果组装、空值处理和 ClassLoader。“已验证的复杂表达式”一节中的完整命令均使用本仓库的 `math-game` 作为目标 JVM，通过真实 Arthas attach 会话验证；线程数、线程名、内存地址等动态结果会因环境不同而变化。

基础命令参数请先阅读 [`ognl`](ognl.md)，`watch` 等命令提供的变量请阅读[表达式核心变量](advice-class.md)。

::: warning
OGNL 在目标 JVM 内执行，调用方法可能读取或修改应用状态。生产环境默认只生成和执行只读表达式，并限制对象数量与展开深度。不要调用应用对象的 `set*`、`add`、`remove`、`clear` 等方法，也不要做文件、网络、进程、ClassLoader、反射提权或线程控制操作。本文对新建临时 `Map` 的 `put` 只修改本次表达式创建的对象。
:::

## 先确认表达式在哪个上下文执行

语法相同不代表可用变量相同。AI Agent 最常见的错误之一，是把其他命令的上下文变量直接放进 `ognl` 命令。

| 使用位置                                             | 可直接使用的根对象或变量                                                                    | 常见错误                                                       |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `ognl` 命令                                          | 静态成员、构造器、字面量和自己定义的 `#变量`                                                | 使用 `params`、`target`、`returnObj` 或 `instances`            |
| `getstatic` 的表达式                                 | 被读取的静态字段值是根对象                                                                  | 把它误当成没有根对象的独立 `ognl` 表达式                       |
| `watch`、`stack`、`monitor`、`tt` 等观察或条件表达式 | `params`、`target`、`returnObj`、`throwExp`、`isReturn` 等 [`Advice`](advice-class.md) 变量 | 在方法进入点读取尚不存在的返回值；在异常点直接调用 `returnObj` |
| `trace` 等条件表达式                                 | 对应命令暴露的 Advice 变量；部分命令还提供 `#cost`                                          | 假设每个命令都有 `#cost`                                       |
| `line --express` / `--condition`                     | Advice 变量以及 `lineNumber`、`localVarMap`、`#cost` 等行级变量                             | 在其他命令中使用 `localVarMap` 或 `lineNumber`                 |
| `vmtool --express`                                   | `instances`，它是 `getInstances` 返回的数组                                                 | 在普通 `ognl` 命令中使用 `instances`                           |

还要区分“完整 Arthas 命令”和“表达式参数”：

```bash
# Arthas 终端需要完整命令，并用外层单引号保护表达式
ognl '#value=@java.lang.System@getProperty("java.version"), #value'
```

如果 MCP、HTTP API 或其他工具已经提供名为 `expression`、`express` 或 `conditionExpress` 的独立参数，只传原始表达式，不要再添加 `ognl`、命令选项、外层单引号或 Markdown 代码围栏：

```text
#value=@java.lang.System@getProperty("java.version"), #value
```

## 稳定的语法积木

| 目的                   | 语法                                         | 说明                                                     |
| ---------------------- | -------------------------------------------- | -------------------------------------------------------- |
| 定义临时变量           | `#name=value`                                | 自定义变量必须带 `#`                                     |
| 顺序执行并返回最后一项 | `#a=1, #b=2, #a+#b`                          | 使用逗号，不要使用分号；分号可能被 Arthas 当成命令分隔符 |
| 访问静态方法或字段     | `@完整类名@method()`、`@完整类名@FIELD`      | 两个 `@` 都不能省略；优先写完整类名                      |
| 创建临时对象           | `new java.util.LinkedHashMap()`              | 不要写 Java 泛型，如 `new HashMap<String,Object>()`      |
| 创建 List              | `{1, 2, 3}`                                  | 结果是一个 List，不是 Java 数组                          |
| 创建 Map               | `#{"name":"arthas", "count":1}`              | `#` 和 `{` 之间不要插入空格或其他字符                    |
| 创建数组               | `new java.lang.String[]{"a", "b"}`           | 需要明确数组类型                                         |
| 集合投影               | `items.{#this.getName()}`                    | 对每一项求值，返回一个新 List                            |
| 集合筛选               | `items.{? #this != null}`                    | 返回所有匹配项                                           |
| 第一项/最后一项筛选    | `items.{^ condition}`、`items.{$ condition}` | 返回的仍是 List；取 `[0]` 前应考虑空结果                 |
| 当前遍历项             | `#this`                                      | 嵌套筛选或投影时含义会随当前层级改变                     |

为了减少属性解析和重载选择的不确定性，复杂表达式中建议显式调用公开 getter，例如使用 `#this.getName()`，而不是 `#this.name`。访问 Map 时建议使用 `get("key")`、`containsKey("key")`，而不是依赖点号属性缩写。

## 已验证的复杂表达式

::: tip
为便于在任意演示 JVM 中复现，本节部分例子使用 `Thread.getAllStackTraces()` 获得集合。它会抓取全部线程的栈；在线程很多的生产 JVM 中，应优先使用 `thread` 等专用命令，或先评估开销，不要高频执行这些示例。
:::

### 筛选、投影、第一项和最后一项

下面的表达式先定义数字列表，再一次返回投影、筛选、第一项和最后一项的结果：

```bash
ognl -x 3 '#numbers={1,2,3,4,5,6}, #{"projection":#numbers.{#this * 10},"selection":#numbers.{? #this % 2 == 0},"first":#numbers.{^ #this % 2 == 0}[0],"last":#numbers.{$ #this % 2 == 0}[0]}'
```

这里已知偶数一定存在，所以可以直接访问 `[0]`。对于运行期集合，应先保存筛选结果并检查是否为空：

```bash
ognl '#matched={1,3,5}.{^ #this % 2 == 0}, #matched.isEmpty() ? null : #matched[0]'
```

### 用临时变量组装结构化结果

长表达式不要重复执行昂贵方法。先保存中间值，最后返回一个新建的 `LinkedHashMap`：

```bash
ognl -x 2 '#result=new java.util.LinkedHashMap(), #props=@java.lang.System@getProperties(), #threads=@java.lang.Thread@getAllStackTraces(), #result.put("javaVersion", #props.get("java.version")), #result.put("javaHome", #props.get("java.home")), #result.put("threadCount", #threads.size()), #result'
```

返回结果的键是 `javaVersion`、`javaHome` 和 `threadCount`。逗号连接的子表达式按顺序求值，整个表达式的值是最后的 `#result`。

### 先筛选，再只返回需要的字段

```bash
ognl '@java.lang.Thread@getAllStackTraces().keySet().{? #this.isAlive() && !#this.isDaemon()}.{#this.getName()}'
```

`.{? ...}` 返回筛选后的线程，后面的 `.{...}` 只投影线程名，避免展开整个 `Thread` 对象。

### 分组计数

下面按线程状态计数。`#counts` 是本次表达式创建的临时 Map，不会修改应用已有对象：

```bash
ognl -x 2 '#counts=new java.util.LinkedHashMap(), @java.lang.Thread@getAllStackTraces().keySet().{#state=#this.getState().toString(), #counts.put(#state, #counts.containsKey(#state) ? #counts.get(#state) + 1 : 1)}, #counts'
```

实际输出类似：

```text
@LinkedHashMap[
    @String[RUNNABLE]:@Integer[10],
    @String[TIMED_WAITING]:@Integer[6],
    @String[WAITING]:@Integer[2],
]
```

不要把 Java Stream 写法 `stream().filter(x -> ...)`、方法引用 `Type::method` 或 `computeIfAbsent(..., x -> ...)` 填进 OGNL。OGNL 不解析 Java lambda；集合筛选和投影应使用 `.{? ...}` 与 `.{...}`。

### 嵌套对象中及时保存 `#this`

在 `entrySet()` 的投影中，`#this` 是当前 `Map.Entry`。先把 key 和 value 保存到有意义的变量，后面的表达式更不容易写错：

```bash
ognl '#all=@java.lang.Thread@getAllStackTraces(), #all.entrySet().{? #this.getKey().getName().startsWith("arthas")}.{#thread=#this.getKey(), #stack=#this.getValue(), #thread.getName() + " | " + #thread.getState() + " | depth=" + #stack.length}'
```

### 空值使用显式分支

OGNL 不支持 JavaScript 风格的 `?.`，也不要混入 SpEL 的 `T(...)` 或模板语法 `${...}`。先保存可能为空的值，再用三元表达式保护方法调用：

```bash
ognl '#value=@java.lang.System@getProperty("arthas.not.exists"), #value == null ? "<missing>" : #value.trim()'
```

返回：

```text
@String[<missing>]
```

### 在 `watch` 中同时处理正常返回和异常

`-f` 会在正常返回和抛出异常后执行表达式。必须先根据 `isReturn` 分支，再访问 `returnObj` 或 `throwExp`：

```bash
watch demo.MathGame primeFactors '#result=new java.util.LinkedHashMap(), #result.put("number", params[0]), #result.put("outcome", isReturn ? "return" : "throw"), #result.put("detail", isReturn ? returnObj.size() : throwExp.getMessage()), #result' -f -n 2 -x 2
```

这个表达式在实际 `math-game` JVM 的正常返回和异常返回路径上都可求值。`-n 2` 限制观察次数，避免命令持续运行。

### 在 `vmtool` 中使用 `instances`

`instances` 只存在于 `vmtool --action getInstances --express` 的表达式上下文：

```bash
vmtool --action getInstances --className java.lang.Thread --limit 100 --express 'instances.{? #this.isAlive() && #this.getName().startsWith("arthas")}.{#this.getName()}'
```

先用 `--limit` 限制实例数量，再筛选和投影。对于实例可能很多的业务类，不要无条件使用 `--limit -1`。

## ClassLoader 不是语法的一部分

访问业务类之前先确认它由哪个 ClassLoader 加载：

```bash
sc -d com.example.OrderService
```

从输出中取得 `classLoaderHash`，再把它作为命令选项传给 `ognl`：

```bash
ognl -c <classLoaderHash> '@com.example.OrderService@STATIC_FIELD'
```

需要注意：

- `-c`/`--classLoaderClass` 是 Arthas 命令选项，不是 OGNL 表达式的一部分。
- 同名类可能由多个 ClassLoader 加载，不要猜测 hash；hash 在 JVM 重启后也可能变化。
- `ClassNotFoundException` 通常应先检查 ClassLoader，而不是随机改写 OGNL 语法。
- `--classLoaderClass` 只有在该类型的 ClassLoader 实例唯一时才适用；有多个实例时使用 hash 精确指定。

## 引号和格式：实际上有两层解析器

Arthas 先解析命令行，随后 OGNL 才解析表达式。若外面还有 Shell、JSON 或 MCP，又会多一层传输格式。

- 在 Arthas 终端中，完整表达式外层使用单引号，OGNL 字符串内部使用双引号。
- 最终命令保持在一个物理行内。终端的视觉自动换行没有问题，但不要在类名、`.`、`@`、`#变量` 或操作符中间插入真实换行。
- 不要在 `.` 后插入空格，例如 `#map. get("key")` 是错误格式。
- 表达式内部用逗号串联步骤，不要用分号。
- 使用 `{$ ...}` 时必须保留外层单引号，否则某些 Shell 会解释 `$`。
- 向 MCP 的表达式参数传值时，不要附加 Markdown 反引号、外层单引号或完整命令。
- 通过 JSON 传输时只按 JSON 规则转义双引号；不要把 Shell 的反斜杠转义原样复制到表达式参数中。

当调用链包含多层引号时，优先使用 Arthas [批处理文件](batch-support.md)或 API 的独立表达式字段，减少 Shell 转义层数。

## 不要把所有失败都当成语法错误

| 现象或异常关键词                                         | 优先检查                                      | 建议动作                                                                               |
| -------------------------------------------------------- | --------------------------------------------- | -------------------------------------------------------------------------------------- |
| `Malformed OGNL expression`、`ExpressionSyntaxException` | 引号、括号、`@类@成员`、逗号、投影/筛选花括号 | 缩减到最小表达式，再逐段添加                                                           |
| `NoSuchPropertyException`                                | 当前命令是否真的提供该变量；属性名是否正确    | 对照上下文变量表，改用明确的公开 getter                                                |
| `MethodFailedException`、`NoSuchMethodException`         | 方法名、参数类型、静态/实例调用方式           | 用 `sm`/`jad` 确认真实签名，不要猜重载                                                 |
| `ClassNotFoundException`                                 | ClassLoader                                   | 先执行 `sc -d`，再指定 `-c`                                                            |
| 中间对象为 `null`                                        | 调用链缺少空值分支                            | 先赋给 `#变量`，用三元表达式保护后续调用                                               |
| `stricter invocation mode`                               | 方法或声明类被 OGNL 严格模式阻止              | 不要关闭保护或尝试反射绕过；改用 `jvm`、`thread`、`sysprop` 等专用命令或安全的公开接口 |
| `module ... does not export`                             | JDK 模块边界，不一定是 OGNL 语法              | 改用 Arthas 专用命令或可访问的公开应用接口                                             |
| 超时、输出巨大或目标 JVM 抖动                            | 表达式工作量和展开范围                        | 先筛选后投影，减少重复调用，设置 `--limit`、`-n` 和合理的 `-x`                         |

例如，`@java.lang.Runtime@getRuntime()` 即使语法正确，在默认的 OGNL 严格调用模式下也可能被拒绝。这类错误不应该通过反复改变 `@`、括号或引号来“碰运气”。

## AI Agent 生成与验证协议

建议把下面的约束加入生成 Arthas 命令的 Agent 提示词：

```text
你正在生成 Arthas 使用的 OGNL。生成前必须确认：
1. 调用方需要“完整 Arthas 命令”还是“原始 expression 参数”，只输出其中一种。
2. 表达式运行在 ognl、getstatic、watch/stack/monitor/tt/trace、line，还是 vmtool 上下文；只使用该上下文真实存在的变量。
3. 业务类先通过 sc -d 确认 ClassLoader；方法和字段不确定时先用 sm/jad 确认，不猜名称或重载。
4. 静态访问统一使用 @完整类名@成员，临时变量统一使用 #name，多个步骤用逗号连接。
5. 不生成 Java lambda/Stream、方法引用、?.、SpEL T(...)、${...}、Java 泛型构造器或分号语句。
6. 默认只读；禁止进程、文件、网络、ClassLoader、反射提权、线程控制和应用状态修改。只允许修改表达式内 new 出来的临时集合。
7. 先筛选再投影，只返回需要的字段，并设置合理的 limit、-n、-x。
8. 最终表达式保持一个物理行；不得在类名、点号、@...@、#变量或操作符内部断行或插入空格。

验证时按以下顺序逐步执行，不要一次提交未经验证的长表达式：
A. 验证最小根对象、静态字段或上下文变量可访问。
B. 每次只增加一个 #变量赋值、方法调用、筛选或投影，并实际执行。
C. 最后再组装 Map/List 输出和限制参数。
D. 失败时先分类为命令/转义、语法、上下文变量、ClassLoader、空值、方法签名、访问限制或资源开销；保留已通过的最小前缀，只修改失败片段。
E. 只有拿到无语法异常的实际 Arthas 返回后，才能声明表达式“已验证”。
```

Agent 在输出前还应做一次机械检查：

- `@类名@成员` 是否有成对的 `@`；
- `()`、`[]`、`{}` 和引号是否配对；
- 每个自定义变量是否带 `#`；
- 投影和筛选中的 `#this` 是否指向预期层级；
- 可能为空的筛选结果、Map value、`returnObj` 或 `throwExp` 是否受分支保护；
- 是否把命令选项误放进表达式，或把完整命令误放进工具的 expression 字段；
- 是否存在被自动格式化拆开的类名、方法名或操作符。

这种“先确认上下文，再小步执行，最后组装输出”的流程，比在失败后整条重写复杂表达式更稳定，也更容易定位真实原因。

## 进一步参考

- [Arthas issue #71：OGNL 特殊用法](https://github.com/alibaba/arthas/issues/71)
- [Apache Commons OGNL Language Guide](https://commons.apache.org/dormant/commons-ognl/language-guide.html)
