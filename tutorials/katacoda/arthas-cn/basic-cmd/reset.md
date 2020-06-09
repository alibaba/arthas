

通过`reset`命令可以重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类

Arthas在 watch/trace 等命令时，实际上是修改了应用的字节码，插入增强的代码。显式执行 `reset`{{execute T2}} 命令，可以清除掉这些增强代码。

## 使用参考

 `reset -h`{{execute T2}}

```bash
$ reset -h
 USAGE:
   reset [-h] [-E] [class-pattern]

 SUMMARY:
   Reset all the enhanced classes

 EXAMPLES:
   reset
   reset *List
   reset -E .*List

 OPTIONS:
 -h, --help                                                         this help
 -E, --regex                                                        Enable regular expression to match (wildcard matching by default)
 <class-pattern>                                                    Path and classname of Pattern Matching
 ```

## 还原指定类

 `trace demo.MathGame primeFactors`{{execute T2}}

 `Ctrl+C (abort)`{{execute interrupt}}

 `reset demo.MathGame`{{execute T2}}

```bash
$ trace demo.MathGame primeFactors
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 94 ms, listenerId: 1
`---ts=2020-06-09 20:01:29;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[1.954979ms] demo.MathGame:primeFactors()

`---ts=2020-06-09 20:01:30;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[0.285145ms] demo.MathGame:primeFactors()
        `---[0.001224ms] throw:java.lang.IllegalArgumentException() #46

`---ts=2020-06-09 20:01:31;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[0.244009ms] demo.MathGame:primeFactors()

`---ts=2020-06-09 20:01:32;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[1.852209ms] demo.MathGame:primeFactors()

`---ts=2020-06-09 20:01:33;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[0.192515ms] demo.MathGame:primeFactors()
        `---[9.01E-4ms] throw:java.lang.IllegalArgumentException() #46

$ reset demo.MathGame
Affect(class count: 1 , method count: 0) cost in 5 ms, listenerId: 0
```

## 还原所有类

 `trace demo.MathGame primeFactors`{{execute T2}}

 `Ctrl+C (abort)`{{execute interrupt}}

 `reset`{{execute T2}}

```bash
$ trace demo.MathGame primeFactors
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 94 ms, listenerId: 1
`---ts=2020-06-09 20:01:29;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[1.954979ms] demo.MathGame:primeFactors()

`---ts=2020-06-09 20:01:30;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[0.285145ms] demo.MathGame:primeFactors()
        `---[0.001224ms] throw:java.lang.IllegalArgumentException() #46

`---ts=2020-06-09 20:01:31;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[0.244009ms] demo.MathGame:primeFactors()

`---ts=2020-06-09 20:01:32;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[1.852209ms] demo.MathGame:primeFactors()

`---ts=2020-06-09 20:01:33;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    `---[0.192515ms] demo.MathGame:primeFactors()
        `---[9.01E-4ms] throw:java.lang.IllegalArgumentException() #46

$ reset
Affect(class-cnt:1 , method-cnt:0) cost in 11 ms.
```
