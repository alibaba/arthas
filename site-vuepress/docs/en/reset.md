reset
===

[`reset` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-reset)

> Reset all classes that have been enhanced by Arthas. These enhanced classes will also be reset when Arthas server is `stop`.

### Usage

```
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

### Reset specified class

```
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

### Reset all classes

```
$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 15 ms.
`---ts=2017-10-26 17:12:06;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    `---[0.128518ms] Test:test()

$ reset
Affect(class-cnt:1 , method-cnt:0) cost in 9 ms.
```
