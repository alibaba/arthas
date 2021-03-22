
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
* [Special usages](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### Check the `out parameters` and `return value`

`watch demo.MathGame primeFactors "{params,returnObj}" -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

`watch demo.MathGame primeFactors "{params,returnObj}" -x 2 -b`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

`watch demo.MathGame primeFactors "{params,target,returnObj}" -x 2 -b -s -n 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

`watch demo.MathGame primeFactors "{params,target}" -x 3`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

`watch demo.MathGame primeFactors "{params[0],target}" "params[0]<0"`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ watch demo.MathGame primeFactors "{params[0],target}" "params[0]<0"
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 68 ms.
ts=2018-12-03 19:36:04; [cost=0.530255ms] result=@ArrayList[
    @Integer[-18178089],
    @MathGame[demo.MathGame@41cf53f9],
]
```

* Only calls that meet the conditions will respond.

* `Watch Express` single value can not be added '{}', and multiple values need to be added '{a, B, C}'.

* `condition Express` cannot add '{}', you can use commas to separate subexpressions and take the last value of the expression to judge.

If there are other overloaded methods with the same name in the watch method, you can filter them by the following methods:

  * Filter according to parameter type

   `watch demo.MathGame primeFactors '{params, params[0].class.name}' 'params[0].class.name == "java.lang.Integer"'`{{execute T2}}

   Press `Q`{{execute T2}} or `Ctrl+C` to abort

  * Filter according to the number of parameters
  
  `watch demo.MathGame primeFactors '{params, params.length}' 'params.length==1'`{{execute T2}}

  Press `Q`{{execute T2}} or `Ctrl+C` to abort

#### Check `exceptions`

`watch demo.MathGame primeFactors "{params[0],throwExp}" -e -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

* `-e`: Trigger when an exception is thrown
* `throwExp`: the exception object

Filter according to exception type or message:

`watch demo.MathGame primeFactors '{params, throwExp}' '#msg=throwExp.toString(), #msg.contains("IllegalArgumentException")' -e -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

#### Filter by time cost

`watch demo.MathGame primeFactors '{params, returnObj}' '#cost>200' -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

`watch demo.MathGame primeFactors 'target'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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

`watch demo.MathGame primeFactors 'target.illegalArgumentCount'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

```bash
$ watch demo.MathGame primeFactors 'target.illegalArgumentCount'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 67 ms.
ts=2018-12-03 20:04:34; [cost=131.303498ms] result=@Integer[8]
ts=2018-12-03 20:04:35; [cost=0.961441ms] result=@Integer[8]
``` 
