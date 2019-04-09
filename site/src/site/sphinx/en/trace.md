trace
=====

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

### Usage

#### Start Demo

Start `arthas-demo` in [Quick Start](quick-start.md).

#### trace method

```bash
$ trace demo.MathGame run
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 42 ms.
`---ts=2018-12-04 00:44:17;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[10.611029ms] demo.MathGame:run()
        +---[0.05638ms] java.util.Random:nextInt()
        +---[10.036885ms] demo.MathGame:primeFactors()
        `---[0.170316ms] demo.MathGame:print()
```

#### Ignore jdk method

```bash
$ trace -j  demo.MathGame run
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 31 ms.
`---ts=2018-12-04 01:09:14;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[5.190646ms] demo.MathGame:run()
        +---[4.465779ms] demo.MathGame:primeFactors()
        `---[0.375324ms] demo.MathGame:print()
```

* `-j`: jdkMethodSkip, skip jdk method trace

#### Filtering by cost

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


#### trace multiple classes or multiple methods

The trace command will only trace the subcalls in the method to the trace, and will not trace down multiple layers. Because traces are expensive, multi-layer traces can lead to a lot of classes and methods that ultimately have to be traced.

You can use the regular expression to match multiple classes and methods on the path to achieve a multi-layer trace effect to some extent.

```bash
Trace -E com.test.ClassA|org.test.ClassB method1|method2|method3
```