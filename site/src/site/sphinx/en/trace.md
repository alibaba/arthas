trace
=====

Track methods calling stack trace and print the time cost in each call.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-express*|condition expression|
|[E]|turn on regx matching while the default is wildcards matching|
|[n:]|calling times|
|#cost|time cost|

**You should know:**
1. any valid [OGNL](https://en.wikipedia.org/wiki/OGNL) expression as `"{params,returnObj}"` supported;
2. filter by time cost as `trace *StringUtils isBlank '#cost>100'`; calling stack with only time cost higher than `100ms` will be printed.
3. `#cost` can be used in `watch/stack/trace`;
4. using `#cost` in Arthas 3.0 instead of `$cost`.
5. `trace` can help to locate the lurking performance issue but only `level-one` method invoking considered.

**Advanced:**
* [Critical fields in expression](advice-class.md)
* [Special usage](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### The first try

```bash
$ trace demo.Demo testListAdd
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 53 ms.
`---ts=2018-09-29 20:05:26;thread_name=Thread-4;id=d;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@659e0bfd
    `---[0.537635ms] demo.Demo:testListAdd()
        +---[0.011367ms] java.lang.StringBuilder:<init>()
        +---[0.009738ms] java.lang.Thread:currentThread()
        +---[0.00751ms] java.lang.Thread:getName()
        +---[min=0.002182ms,max=0.010155ms,total=0.012337ms,count=2] java.lang.StringBuilder:append()
        +---[0.005855ms] java.lang.StringBuilder:toString()
        +---[0.027382ms] java.io.PrintStream:println()
        +---[min=0.001712ms,max=0.005407ms,total=0.007119ms,count=2] java.util.ArrayList:<init>()
        +---[min=0.00176ms,max=0.009703ms,total=0.015103ms,count=4] java.util.List:add()
        `---[0.233937ms] demo.Demo:addTwoLists()`
```

#### Filtering by time cost

```bash
$ trace demo.Demo testListAdd #cost>0.5
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 99 ms.
`---ts=2018-09-29 20:07:51;thread_name=Thread-0;id=9;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@659e0bfd
    `---[0.719686ms] demo.Demo:testListAdd()
        +---[0.053ms] java.lang.StringBuilder:<init>()
        +---[0.008896ms] java.lang.Thread:currentThread()
        +---[0.006468ms] java.lang.Thread:getName()
        +---[min=0.001662ms,max=0.006394ms,total=0.008056ms,count=2] java.lang.StringBuilder:append()
        +---[0.00581ms] java.lang.StringBuilder:toString()
        +---[0.032904ms] java.io.PrintStream:println()
        +---[min=8.44E-4ms,max=0.005536ms,total=0.00638ms,count=2] java.util.ArrayList:<init>()
        +---[min=8.46E-4ms,max=0.009082ms,total=0.011858ms,count=4] java.util.List:add()
        `---[0.24375ms] demo.Demo:addTwoLists()`
```

Only the calling trace of the time cost higher than `0.5ms`presented now.

**You should know:**
1. like JProfile and other similar commercial softwares, you can `trace` down the specified method calling stack with time cost in Arthas;
2. there will be some overhead using `trace` but not much;
3. the time cost is an instructive clue for troubleshooting; it's not that accurate by ignoring the cost it (Arthas) itself causes; the deeper or more the call is, the worse accuracy the time cost will be;
4. `[0,0,0ms,11]xxx:yyy() [throws Exception]`，the same method calling aggregated into one line here while `throws Exception` indicates there is an exception.

