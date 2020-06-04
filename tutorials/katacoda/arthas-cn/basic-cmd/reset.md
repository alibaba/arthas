

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

```bash
$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 57 ms.
`---ts=2017-10-26 17:10:33;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    `---[0.590102ms] Test:test()

`---ts=2017-10-26 17:10:34;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    `---[0.068692ms] Test:test()

$ reset Test
Affect(class-cnt:1 , method-cnt:0) cost in 11 ms.
```

## 还原所有类

```bash
$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 57 ms.
`---ts=2017-10-26 17:10:33;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    `---[0.590102ms] Test:test()

`---ts=2017-10-26 17:10:34;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    `---[0.068692ms] Test:test()

$ reset Test
Affect(class-cnt:1 , method-cnt:0) cost in 11 ms.
```
