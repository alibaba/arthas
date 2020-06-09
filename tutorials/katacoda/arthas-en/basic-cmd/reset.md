

`reset` can reset enhanced classes. All enhanced classes will be reset to their original states. When Arthas server closes, all these enhanced classes will be reset too.

When Arthas executes commands such as watch/trace, it actually modifies the application's bytecode and inserts the enhanced code. These enhancement codes can be removed by explicitly executing the `reset`{{execute T2}} command.

## Usage

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

## Reset specified class

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

## Reset all classes

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
