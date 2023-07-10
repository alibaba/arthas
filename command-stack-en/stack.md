
> Print out the full call stack of the current method.

Most often we know one method gets called, but we have no idea on which code path gets executed or when the method gets called since there are so many code paths to the target method. The command `stack` comes to rescue in this difficult situation.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-expression*|condition expression|
|`[E]`|turn on regex match, the default behavior is wildcard match|
|`[n:]`|execution times|

There's one thing worthy noting here is observation expression. The observation expression supports OGNL grammar, for example, you can come up a expression like this `"{params,returnObj}"`. All OGNL expressions are supported as long as they are legal to the grammar.

Thanks for `advice`'s data structure, it is possible to observe from varieties of different angles. Inside `advice` parameter, all necessary information for notification can be found.

* Pls. also refer to [https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71) for more advanced usage
* OGNL official site: [https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### stack

`stack demo.MathGame primeFactors`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ stack demo.MathGame primeFactors
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 36 ms.
ts=2018-12-04 01:32:19;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    @demo.MathGame.run()
        at demo.MathGame.main(MathGame.java:16)
```

#### Filtering by condition expression

`stack demo.MathGame primeFactors 'params[0]<0' -n 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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


#### Filtering by cost

`stack demo.MathGame primeFactors '#cost>5'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ stack demo.MathGame primeFactors '#cost>5'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 35 ms.
ts=2018-12-04 01:35:58;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    @demo.MathGame.run()
        at demo.MathGame.main(MathGame.java:16)
```


