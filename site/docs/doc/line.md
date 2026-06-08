# line

::: tip
在指定源码行观察方法入参、局部变量和表达式结果。
:::

`line` 命令会在目标方法的指定源码行插入探针。当执行到该行时，Arthas 会输出当前线程、耗时、行号以及 OGNL 表达式结果。它适合排查“方法内部某一行变量值不符合预期”的问题。

::: warning
`line` 通过字节码增强实现。线上或预发环境使用时，请尽量明确 `--class`、`--method`、`--desc` 和 `--line`，并使用 `-n` 限制输出次数。诊断结束后可以执行 `reset` 或 `stop` 还原增强过的类。
:::

## 参数说明

| 参数名称 | 参数说明 |
| ---: | :--- |
| `--class <class-pattern>` | 需要观察的类名，必填，默认通配符匹配 |
| `--method <method-pattern>` | 需要观察的方法名，可选，不指定时匹配类里的所有可插桩方法 |
| `--desc <method-desc>` | JVM 方法描述符，可选，用于区分重载方法，比如 `(I)Ljava/util/List;` |
| `--line <line>` | 需要观察的源码行号，支持逗号分隔和重复指定，比如 `--line 51,57 --line 61` |
| `--express <express>` | 观察表达式，默认值是 `{params, localVarMap}` |
| `--condition <express>` | 条件表达式，条件成立时才输出 |
| `--list-lines` | 只列出匹配方法的可用行号，不增强类 |
| `[E]` | 开启正则表达式匹配，默认是通配符匹配 |
| `[x:]` | 输出对象展开层级，默认值为 1 |
| `[M:]` | 输出结果大小限制，默认使用 `options` 中的 `object-size-limit` |
| `[n:]` | 执行次数限制，默认值为 100 |
| `[c:]` | 指定 ClassLoader hash，只增强该 ClassLoader 加载的类 |
| `[m <arg>]` | 指定 Class 最大匹配数量，默认值为 50。长格式为 `[maxMatch <arg>]` |
| `--stack` | 命中行号时输出当前线程栈 |
| `--stack-depth <depth>` | `--stack` 的最大栈深度，默认值为 32，最大值为 256 |

## 表达式变量

`line` 命令复用 Arthas 的 OGNL 表达式机制。常用变量如下：

| 变量 | 说明 |
| ---: | :--- |
| `params` | 方法入参数组 |
| `target` | 当前对象，静态方法为 `null` |
| `clazz` | 当前类 |
| `method` | 当前方法信息 |
| `lineNumber` | 当前命中的源码行号 |
| `argNames` | 方法参数名数组，依赖目标类保留调试信息 |
| `localVars` / `locals` | 当前行可见的局部变量值数组 |
| `localVarNames` | 当前行可见的局部变量名数组 |
| `localVarMap` | 局部变量名到变量值的映射 |
| `#cost` | 从方法入口到当前行的耗时，单位为 ms |

局部变量名和值依赖目标类编译时保留 `LocalVariableTable` 调试信息。如果目标类没有调试信息，`localVarMap` 可能为空或不完整。

## 使用示例

### 启动 Demo

启动[快速入门](quick-start.md)里的 `math-game`。

### 查看可用行号

先使用 `--list-lines` 找到目标方法中可以插桩的源码行：

```bash
$ line --list-lines --class demo.MathGame --method primeFactors
class=demo.MathGame source=MathGame.java
method=primeFactors(I)Ljava/util/List; lines=[44, 45, 46, 49, 50, 51, 52, 53, 54, 55, 57, 61]
```

### 观察指定行的入参和局部变量

默认表达式为 `{params, localVarMap}`：

```bash
$ line --class demo.MathGame --method primeFactors --line 51 -n 2 -x 2
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 32 ms, listenerId: 1
ts=2026-06-08 01:45:56.111; [thread=main(3) cost=0.937167ms] demo.MathGame.primeFactors(I)Ljava/util/List;:51
result=@ArrayList[
    @Object[][
        @Integer[122542],
    ],
    @LinkedHashMap[
        @String[number]:@Integer[122542],
        @String[result]:@ArrayList[isEmpty=true;size=0],
        @String[i]:@Integer[2],
    ],
]
Command execution times exceed limit: 2, so command will exit. You can set it with -n option.
```

### 一次观察多个行号

`--line` 支持逗号分隔和重复指定：

```bash
line --class demo.MathGame --method primeFactors --line 51,57 --express '{lineNumber, localVarMap}' -n 4 -x 2
```

第一版只支持精确行号列表，不支持 `51-57` 这类范围语法。

### 使用条件表达式过滤

只在局部变量 `i` 大于 10 时输出：

```bash
line --class demo.MathGame --method primeFactors --line 51 \
  --condition 'localVarMap["i"] != null && localVarMap["i"] > 10' \
  --express '{params[0], localVarMap["i"], localVarMap["result"]}' -n 1 -x 2
```

### 指定方法描述符

当方法重载时，建议使用 `--desc` 精确指定 JVM 方法描述符：

```bash
line --class demo.MathGame --method primeFactors --desc '(I)Ljava/util/List;' --line 51
```

可以通过 `sm -d demo.MathGame primeFactors` 查看方法描述符。

### 输出当前线程栈

命中行号时输出当前线程栈：

```bash
line --class demo.MathGame --method primeFactors --line 57 --stack --stack-depth 8 -n 1
```

## 注意事项

- `--line` 传入的行号必须大于 0，并且最多指定 256 个不同的行号。
- `line` 会跳过构造方法、类初始化方法、native 方法和 abstract 方法。
- 如果省略 `--method`，同一个类里多个包含目标行号的方法都可能被增强，建议先用 `--list-lines` 和 `--desc` 收敛目标。
- 如果目标类没有 `LineNumberTable`，`--list-lines` 不会列出可用行号，`line` 也无法在源码行插入探针。
- 重复执行同一个行号不会重复插入相同探针；新增行号时会尽量只追加缺失的行号探针。
