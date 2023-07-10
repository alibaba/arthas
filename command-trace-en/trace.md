
> Trace method calling path, and output the time cost for each node in the path.

`trace` can track the calling path specified by `class-pattern` / `method-pattern`, and calculate the time cost on the whole path.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-express*|condition expression|
|`[E]`|enable regex match, the default behavior is wildcards match|
|`[n:]`|execution times|
|#cost|time cost|

There's one thing worthy noting here is observation expression. The observation expression supports OGNL grammar, for example, you can come up a expression like this `"{params,returnObj}"`. All OGNL expressions are supported as long as they are legal to the grammar.

Thanks for `advice`'s data structure, it is possible to observe from varieties of different angles. Inside `advice` parameter, all necessary information for notification can be found.

Pls. refer to [core parameters in expression](advice-class.md) for more details.
* Pls. also refer to [https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71) for more advanced usage
* OGNL official site: [https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

Many times what we are interested is the exact trace result when the method call takes time over one particular period. It is possible to achieve this in Arthas, for example: `trace *StringUtils isBlank '#cost>100'` means trace result will only be output when the executing time exceeds 100ms.


> `watch`/`stack`/`trace`, these three commands all support `#cost`.

### Notice

`trace` is handy to help discovering and locating the performance flaws in your system, but pls. note Arthas can only trace the first level method call each time.


After version 3.3.0, you can use the Dynamic Trace feature to add new matching classes/methods, see the following example.

### Usage

#### Trace method

`trace demo.MathGame run`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ trace demo.MathGame run
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 28 ms.
`---ts=2019-12-04 00:45:08;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[0.617465ms] demo.MathGame:run()
        `---[0.078946ms] demo.MathGame:primeFactors() #24 [throws Exception]

`---ts=2019-12-04 00:45:09;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[1.276874ms] demo.MathGame:run()
        `---[0.03752ms] demo.MathGame:primeFactors() #24 [throws Exception]
```


#### Trace times limit

If the method invoked many times, use `-n` options to specify trace times. For example, the command will exit when received a trace result.

`trace demo.MathGame run -n 1`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ trace demo.MathGame run -n 1
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 20 ms.
`---ts=2019-12-04 00:45:53;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[0.549379ms] demo.MathGame:run()
        +---[0.059839ms] demo.MathGame:primeFactors() #24
        `---[0.232887ms] demo.MathGame:print() #25

Command execution times exceed limit: 1, so command will exit. You can set it with -n option.
```

#### Include jdk method

* `--skipJDKMethod <value> `   skip jdk method trace, default value true.

`trace --skipJDKMethod false demo.MathGame run`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ trace --skipJDKMethod false demo.MathGame run
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 60 ms.
`---ts=2019-12-04 00:44:41;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[1.357742ms] demo.MathGame:run()
        +---[0.028624ms] java.util.Random:nextInt() #23
        +---[0.045534ms] demo.MathGame:primeFactors() #24 [throws Exception]
        +---[0.005372ms] java.lang.StringBuilder:<init>() #28
        +---[0.012257ms] java.lang.Integer:valueOf() #28
        +---[0.234537ms] java.lang.String:format() #28
        +---[min=0.004539ms,max=0.005778ms,total=0.010317ms,count=2] java.lang.StringBuilder:append() #28
        +---[0.013777ms] java.lang.Exception:getMessage() #28
        +---[0.004935ms] java.lang.StringBuilder:toString() #28
        `---[0.06941ms] java.io.PrintStream:println() #28

`---ts=2019-12-04 00:44:42;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[3.030432ms] demo.MathGame:run()
        +---[0.010473ms] java.util.Random:nextInt() #23
        +---[0.023715ms] demo.MathGame:primeFactors() #24 [throws Exception]
        +---[0.005198ms] java.lang.StringBuilder:<init>() #28
        +---[0.006405ms] java.lang.Integer:valueOf() #28
        +---[0.178583ms] java.lang.String:format() #28
        +---[min=0.011636ms,max=0.838077ms,total=0.849713ms,count=2] java.lang.StringBuilder:append() #28
        +---[0.008747ms] java.lang.Exception:getMessage() #28
        +---[0.019768ms] java.lang.StringBuilder:toString() #28
        `---[0.076457ms] java.io.PrintStream:println() #28
```

#### Filtering by cost

`trace demo.MathGame run '#cost > 10'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ trace demo.MathGame run '#cost > 10'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 41 ms.
`---ts=2018-12-04 01:12:02;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[12.033735ms] demo.MathGame:run()
        +---[0.006783ms] java.util.Random:nextInt()
        +---[11.852594ms] demo.MathGame:primeFactors()
        `---[0.05447ms] demo.MathGame:print()
```

> Only the call path which's time cost is higher than `10ms` will be shown. This feature is handy to focus on what's needed to focus when troubleshoot.

* Here Arthas provides the similar functionality JProfile and other commercial software provide. Compared to these professional softwares, Arthas doesn't deduce the time cost `trace` itself takes, therefore it is not as accurate as these softwares offer. More classes and methods on the calling path, more inaccurate `trace` output is, but it is still helpful for diagnostics where the bottleneck is.
* "[12.033735ms]" means the method on the node takes `12.033735` ms.
* "[min=0.005428ms,max=0.094064ms,total=0.105228ms,count=3] demo:call()" means aggregating all same method calls into one single line. The minimum time cost is `0.005428` ms, the maximum time cost is `0.094064` ms, and the total time cost for all method calls (`3` times in total) to "demo:call()" is `0.105228ms`. If "throws Exception" appears in this line, it means some exceptions have been thrown from this method calls.
* The total time cost may not equal to the sum of the time costs each sub method call takes, this is because Arthas instrumented code takes time too.


#### Trace multiple classes or multiple methods

The trace command will only trace the subcalls in the method to the trace, and will not trace down multiple layers. Because traces are expensive, multi-layer traces can lead to a lot of classes and methods that ultimately have to be traced.

You can use the regular expression to match multiple classes and methods on the path to achieve a multi-layer trace effect to some extent.

```bash
Trace -E com.test.ClassA|org.test.ClassB method1|method2|method3
```


#### Dynamic trace

> Supported since version 3.3.0.

Open terminal 1, trace the `run` method, and you can see the printout `listenerId: 1` .

`trace demo.MathGame run`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
[arthas@59161]$ trace demo.MathGame run
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 112 ms, listenerId: 1
`---ts=2020-07-09 16:48:11;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[1.389634ms] demo.MathGame:run()
        `---[0.123934ms] demo.MathGame:primeFactors() #24 [throws Exception]

`---ts=2020-07-09 16:48:12;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[3.716391ms] demo.MathGame:run()
        +---[3.182813ms] demo.MathGame:primeFactors() #24
        `---[0.167786ms] demo.MathGame:print() #25
```

Now to drill down into the sub method `primeFactors`, you can open a new terminal 2 and use the `telnet localhost 3658` connects to the arthas, then trace `primeFactors` with the specify `listenerId`.

`trace demo.MathGame primeFactors --listenerId 1`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
[arthas@59161]$ trace demo.MathGame primeFactors --listenerId 1
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 34 ms, listenerId: 1
```

At Terminal 2 prints the results, indicating that a method has been enhanced: `Affect(class count: 1 , method count: 1)`, but no more results are printed.

At terminal 1, you can see that the trace result has increased by one layer:

```bash
`---ts=2020-07-09 16:49:29;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[0.492551ms] demo.MathGame:run()
        `---[0.113929ms] demo.MathGame:primeFactors() #24 [throws Exception]
            `---[0.061462ms] demo.MathGame:primeFactors()
                `---[0.001018ms] throw:java.lang.IllegalArgumentException() #46

`---ts=2020-07-09 16:49:30;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[0.409446ms] demo.MathGame:run()
        +---[0.232606ms] demo.MathGame:primeFactors() #24
        |   `---[0.1294ms] demo.MathGame:primeFactors()
        `---[0.084025ms] demo.MathGame:print() #25
```

Dynamic trace by specifying `listenerId`, you can go deeper and deeper. In addition, commands such as `watch`/`tt`/`monitor` also support similar functionality.