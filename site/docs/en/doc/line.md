# line

::: tip
Watch method arguments, local variables, and expression results at specified source line numbers.
:::

The `line` command inserts probes at specified source line numbers in matched methods. When execution reaches one of these lines, Arthas prints the current thread, elapsed time, line number, and OGNL expression result. It is useful when a value inside a method is unexpected at a specific source line.

::: warning
`line` works through bytecode enhancement. In production or staging environments, specify `--class`, `--method`, `--desc`, and `--line` as precisely as possible, and use `-n` to limit output. Run `reset` or `stop` after diagnosis to restore enhanced classes.
:::

## Parameters

|                        Name | Specification                                                                                                            |
| --------------------------: | :----------------------------------------------------------------------------------------------------------------------- |
|   `--class <class-pattern>` | Class name to watch. Required. Wildcard matching is used by default                                                      |
| `--method <method-pattern>` | Method name to watch. Optional. If omitted, all instrumentable methods in the class may match                            |
|      `--desc <method-desc>` | JVM method descriptor. Optional. Use it to select an overloaded method, for example `(I)Ljava/util/List;`                |
|             `--line <line>` | Source line numbers to watch. Supports comma-separated values and repeated options, for example `--line 51,57 --line 61` |
|       `--express <express>` | Expression to evaluate. The default value is `{params, localVarMap}`                                                     |
|     `--condition <express>` | Condition expression. Output is printed only when the condition is true                                                  |
|              `--list-lines` | List available source line numbers without enhancing classes                                                             |
|                       `[E]` | Enable regular expression matching. Wildcard matching is used by default                                                 |
|                      `[x:]` | Object expansion level. The default value is 1                                                                           |
|                      `[M:]` | Result size limit. The default value comes from `options` `object-size-limit`                                            |
|                      `[n:]` | Execution count limit. The default value is 100                                                                          |
|                      `[c:]` | Specify the ClassLoader hash. Only classes loaded by this ClassLoader are enhanced                                       |
|                 `[m <arg>]` | Specify the maximum number of matched classes. The default value is 50. The long form is `[maxMatch <arg>]`              |
|                   `--stack` | Print the current stack trace when the line probe hits                                                                   |
|     `--stack-depth <depth>` | Maximum stack depth for `--stack`. The default value is 32, and the maximum value is 256                                 |

## Expression Variables

`line` reuses the Arthas OGNL expression mechanism. Common variables:

|               Variable | Description                                                                  |
| ---------------------: | :--------------------------------------------------------------------------- |
|               `params` | Method argument array                                                        |
|               `target` | Current object. It is `null` for static methods                              |
|                `clazz` | Current class                                                                |
|               `method` | Current method information                                                   |
|           `lineNumber` | Matched source line number                                                   |
|             `argNames` | Method argument name array. It depends on debug metadata in the target class |
| `localVars` / `locals` | Local variable value array visible at the current line                       |
|        `localVarNames` | Local variable name array visible at the current line                        |
|          `localVarMap` | Map from local variable name to value                                        |
|                `#cost` | Elapsed time from method entry to the current line, in milliseconds          |

Local variable names and values depend on the target bytecode retaining `LocalVariableTable` debug metadata. If the target class has no debug metadata, `localVarMap` may be empty or incomplete.

## Usage

### Start Demo

Start `math-game` in [Quick Start](quick-start.md).

### List Available Line Numbers

Use `--list-lines` first to find source lines that can be instrumented:

```bash
$ line --list-lines --class demo.MathGame --method primeFactors
class=demo.MathGame source=MathGame.java
method=primeFactors(I)Ljava/util/List; lines=[44, 45, 46, 49, 50, 51, 52, 53, 54, 55, 57, 61]
```

### Watch Arguments and Local Variables at a Line

The default expression is `{params, localVarMap}`:

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

### Watch Multiple Line Numbers

`--line` supports comma-separated values and repeated options:

```bash
line --class demo.MathGame --method primeFactors --line 51,57 --express '{lineNumber, localVarMap}' -n 4 -x 2
```

The first version supports exact line number lists only. Range syntax such as `51-57` is not supported.

### Filter with a Condition Expression

Print output only when local variable `i` is greater than 10:

```bash
line --class demo.MathGame --method primeFactors --line 51 \
  --condition 'localVarMap["i"] != null && localVarMap["i"] > 10' \
  --express '{params[0], localVarMap["i"], localVarMap["result"]}' -n 1 -x 2
```

### Specify Method Descriptor

When a method is overloaded, use `--desc` to select the exact JVM method descriptor:

```bash
line --class demo.MathGame --method primeFactors --desc '(I)Ljava/util/List;' --line 51
```

You can use `sm -d demo.MathGame primeFactors` to inspect method descriptors.

### Print Current Stack Trace

Print the current stack trace when the line probe hits:

```bash
line --class demo.MathGame --method primeFactors --line 57 --stack --stack-depth 8 -n 1
```

## Notes

- `--line` values must be greater than 0. At most 256 distinct line numbers can be specified.
- `line` skips constructors, class initializers, native methods, and abstract methods.
- If `--method` is omitted, multiple methods in the same class may be enhanced when they contain the target line number. Use `--list-lines` and `--desc` to narrow the target.
- If the target class has no `LineNumberTable`, `--list-lines` cannot list available lines and `line` cannot insert probes by source line.
- Running the same line probe repeatedly does not insert duplicate probes. When new line numbers are added, Arthas tries to append only missing line probes.
