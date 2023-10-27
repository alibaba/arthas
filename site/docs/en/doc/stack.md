# stack

[`stack` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-stack)

::: tip
Print out the full call stack of the current method.
:::

Most often we know one method gets called, but we have no idea on which code path gets executed or when the method gets called since there are so many code paths to the target method. The command `stack` comes to rescue in this difficult situation.

## Parameters

|                   Name | Specification                                                                                          |
| ---------------------: | :----------------------------------------------------------------------------------------------------- |
|        _class-pattern_ | pattern for the class name                                                                             |
|       _method-pattern_ | pattern for the method name                                                                            |
| _condition-expression_ | condition expression                                                                                   |
|                  `[E]` | turn on regex match, the default behavior is wildcard match                                            |
|                 `[n:]` | execution times                                                                                        |
|            `[m <arg>]` | Specify the max number of matched Classes, the default value is 50. Long format is `[maxMatch <arg>]`. |

There's one thing worthy noting here is observation expression. The observation expression supports OGNL grammar, for example, you can come up a expression like this `"{params,returnObj}"`. All OGNL expressions are supported as long as they are legal to the grammar.

Thanks for `advice`'s data structure, it is possible to observe from varieties of different angles. Inside `advice` parameter, all necessary information for notification can be found.

Pls. refer to [core parameters in expression](advice-class.md) for more details.

- Pls. also refer to [https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71) for more advanced usage
- OGNL official site: [https://commons.apache.org/dormant/commons-ognl/language-guide.html](https://commons.apache.org/dormant/commons-ognl/language-guide.html)

## Usage

### Start Demo

Start `math-game` in [Quick Start](quick-start.md).

### stack

```bash
$ stack demo.MathGame primeFactors
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 36 ms.
ts=2018-12-04 01:32:19;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    @demo.MathGame.run()
        at demo.MathGame.main(MathGame.java:16)
```

### Specify the max number of matched Classes

```bash
$ stack demo.MathGame primeFactors -m 1
Press Q or Ctrl+C to abort.
Affect(class count:1 , method count:1) cost in 561 ms, listenerId: 5.
ts=2022-12-25 21:07:07;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@b4aac2
    @demo.MathGame.primeFactors()
        at demo.MathGame.run(MathGame.java:46)
        at demo.MathGame.main(MathGame.java:38)
```

### Filtering by condition expression

```bash
$ stack demo.MathGame primeFactors 'params[0]<0' -n 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 30 ms.
ts=2018-12-04 01:34:27;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    @demo.MathGame.run()
        at demo.MathGame.main(MathGame.java:16)

ts=2018-12-04 01:34:30;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    @demo.MathGame.run()
        at demo.MathGame.main(MathGame.java:16)

Command execution times exceed limit: 2, so command will exit. You can set it with -n option.
```

### Filtering by cost

```bash
$ stack demo.MathGame primeFactors '#cost>5'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 35 ms.
ts=2018-12-04 01:35:58;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    @demo.MathGame.run()
        at demo.MathGame.main(MathGame.java:16)
```
