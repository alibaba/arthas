watch
=====

[`watch` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-watch)

Monitor methods in data aspect including `return values`, `exceptions` and `parameters`.

With the help of [OGNL](https://commons.apache.org/proper/commons-ognl/index.html), you can easily check the details of variables when methods being invoked.

### Parameters & Options

There are four different scenarios for `watch` command, which makes it rather complicated. 

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*expression*|expression to monitor|
|*condition-expression*|condition expression to filter|
|[b]|before method being invoked|
|[e]|when method encountering exceptions|
|[s]|when method exits normally|
|[f]|when method exits (either succeed or fail with exceptions)|
|[E]|turn on regex matching while the default is wildcard matching|
|[x:]|the depth to print the specified property with default value: 1|

F.Y.I
1. any valid OGNL expression as `"{params,returnObj}"` supported
2. there are four *watching* points: `-b`, `-e`, `-s` and `-f` (the first three are off in default while `-f` on);
3. at the *watching* point, Arthas will use the *expression* to evaluate the variables and print them out;
4. `in parameters` and `out parameters` are different since they can be modified within the invoked methods; `params` stands for `in parameters` in `-b`while `out parameters` in other *watching* points;
5. there are no `return values` and `exceptions` when using `-b`.


Advanced:
* [Critical fields in *expression*](advice-class.md)
* [Special usages](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### Start Demo

Start `math-game` in [Quick Start](quick-start.md).

#### Check the `out parameters` and `return value`

```bash
$ watch demo.MathGame primeFactors "{params,returnObj}" -x 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 44 ms.
ts=2018-12-03 19:16:51; [cost=1.280502ms] result=@ArrayList[
    @Object[][
        @Integer[1],
    ],
    @ArrayList[
        @Integer[3],
        @Integer[19],
        @Integer[191],
        @Integer[49199],
    ],
]
```

#### Check `in parameters`

```bash
$ watch demo.MathGame primeFactors "{params,returnObj}" -x 2 -b
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 50 ms.
ts=2018-12-03 19:23:23; [cost=0.0353ms] result=@ArrayList[
    @Object[][
        @Integer[-1077465243],
    ],
    null,
]
```

Compared to the previous *check*: 

* `return value` is `null` since it's `-b`.


#### Check *before* and *after* at the same time


```bash
$ watch demo.MathGame primeFactors "{params,target,returnObj}" -x 2 -b -s -n 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 46 ms.
ts=2018-12-03 19:29:54; [cost=0.01696ms] result=@ArrayList[
    @Object[][
        @Integer[1],
    ],
    @MathGame[
        random=@Random[java.util.Random@522b408a],
        illegalArgumentCount=@Integer[13038],
    ],
    null,
]
ts=2018-12-03 19:29:54; [cost=4.277392ms] result=@ArrayList[
    @Object[][
        @Integer[1],
    ],
    @MathGame[
        random=@Random[java.util.Random@522b408a],
        illegalArgumentCount=@Integer[13038],
    ],
    @ArrayList[
        @Integer[2],
        @Integer[2],
        @Integer[2],
        @Integer[5],
        @Integer[5],
        @Integer[73],
        @Integer[241],
        @Integer[439],
    ],
]
```

F.Y.I

* `-n 2`: threshold of execution times is 2.
* the first block of output is the *before watching* point;
* *the order of the output determined by the *watching* order itself (nothing to do with the order of the options `-b -s`).

#### Use `-x` to check more details

```bash
$ watch demo.MathGame primeFactors "{params,target}" -x 3
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 58 ms.
ts=2018-12-03 19:34:19; [cost=0.587833ms] result=@ArrayList[
    @Object[][
        @Integer[1],
    ],
    @MathGame[
        random=@Random[
            serialVersionUID=@Long[3905348978240129619],
            seed=@AtomicLong[3133719055989],
            multiplier=@Long[25214903917],
            addend=@Long[11],
            mask=@Long[281474976710655],
            DOUBLE_UNIT=@Double[1.1102230246251565E-16],
            BadBound=@String[bound must be positive],
            BadRange=@String[bound must be greater than origin],
            BadSize=@String[size must be non-negative],
            seedUniquifier=@AtomicLong[-3282039941672302964],
            nextNextGaussian=@Double[0.0],
            haveNextNextGaussian=@Boolean[false],
            serialPersistentFields=@ObjectStreamField[][isEmpty=false;size=3],
            unsafe=@Unsafe[sun.misc.Unsafe@2eaa1027],
            seedOffset=@Long[24],
        ],
        illegalArgumentCount=@Integer[13159],
    ],
]
```

* `-x`: Expand level of object (1 by default)

#### Use condition expressions to locate specific call

```bash
$ watch demo.MathGame primeFactors "{params[0],target}" "params[0]<0"
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 68 ms.
ts=2018-12-03 19:36:04; [cost=0.530255ms] result=@ArrayList[
    @Integer[-18178089],
    @MathGame[demo.MathGame@41cf53f9],
]
```

#### Check `exceptions`

```bash
$ watch demo.MathGame primeFactors "{params[0],throwExp}" -e -x 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 62 ms.
ts=2018-12-03 19:38:00; [cost=1.414993ms] result=@ArrayList[
    @Integer[-1120397038],
    java.lang.IllegalArgumentException: number is: -1120397038, need >= 2
	at demo.MathGame.primeFactors(MathGame.java:46)
	at demo.MathGame.run(MathGame.java:24)
	at demo.MathGame.main(MathGame.java:16)
,
]
```

* `-e`: Trigger when an exception is thrown
* `throwExp`: the exception object

#### Filter by time cost

```bash
$ watch demo.MathGame primeFactors '{params, returnObj}' '#cost>200' -x 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 66 ms.
ts=2018-12-03 19:40:28; [cost=2112.168897ms] result=@ArrayList[
    @Object[][
        @Integer[1],
    ],
    @ArrayList[
        @Integer[5],
        @Integer[428379493],
    ],
]
```

* `#cost>200` (`ms`) filter out all invokings that take less than `200ms`.


#### Check the field of the target object

* `target` is the `this` object in java.

```bash
$ watch demo.MathGame primeFactors 'target'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 52 ms.
ts=2018-12-03 19:41:52; [cost=0.477882ms] result=@MathGame[
    random=@Random[java.util.Random@522b408a],
    illegalArgumentCount=@Integer[13355],
]
```

* `target.field_name`: the field of the current object.

```bash
$ watch demo.MathGame primeFactors 'target.illegalArgumentCount'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 67 ms.
ts=2018-12-03 20:04:34; [cost=131.303498ms] result=@Integer[8]
ts=2018-12-03 20:04:35; [cost=0.961441ms] result=@Integer[8]
``` 

#### Get a static field and calling a static method 

```bash
watch demo.MathGame * '{params,@demo.MathGame@random.nextInt(100)}' -v -n 1 -x 2
[arthas@6527]$ watch demo.MathGame * '{params,@demo.MathGame@random.nextInt(100)}' -n 1 -x 2
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 5) cost in 34 ms, listenerId: 3
ts=2021-01-05 21:35:20; [cost=0.173966ms] result=@ArrayList[
    @Object[][
        @Integer[-138282],
    ],
    @Integer[89],
]
```

* Note that here you use `Thread.currentThread().getContextClassLoader()` to load, and it is better to use the exact `classloader` [ognl](ognl.md).


#### Exclude the specified class

> The watch/trace/monitor/stack/tt commands all support the `--exclude-class-pattern` parameter

Use the `--exclude-class-pattern` parameter to exclude the specified class, for example:

```bash
watch javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter
```
#### Does not match subclass

By default, the watch/trace/monitor/stack/tt commands will match subclass. If you don't want to match, you can turn it off.

```bash
options disable-sub-class true
```


#### Use the -v parameter to print more information

> The watch/trace/monitor/stack/tt commands all support the `-v` parameter.

When the command is executed, there is no output result. There are two possibilities:

1. The matched function is not executed
2. The result of the conditional expression is false

But the user cannot tell which situation is.

Using the `-v` option, the specific value and execution result of `Condition express` will be printed for easy confirmation.

such as:

```
$ watch -v -x 2 demo.MathGame print 'params' 'params[0] > 100000'
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 29 ms, listenerId: 11
Condition express: params[0] > 100000 , result: false
Condition express: params[0] > 100000 , result: false
Condition express: params[0] > 100000 , result: true
ts=2020-12-02 22:38:56; [cost=0.060843ms] result=@Object[][
    @Integer[1],
    @ArrayList[
        @Integer[200033],
    ],
]
Condition express: params[0] > 100000 , result: true
ts=2020-12-02 22:38:57; [cost=0.052877ms] result=@Object[][
    @Integer[1],
    @ArrayList[
        @Integer[29],
        @Integer[4243],
    ],
]
```