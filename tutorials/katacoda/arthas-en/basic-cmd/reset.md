

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

## Reset all classes

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
