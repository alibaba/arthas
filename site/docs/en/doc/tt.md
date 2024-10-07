# tt

[`tt` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-tt)

Check the `parameters`, `return values` and `exceptions` of the methods at different times.

`watch` is a powerful command but due to its feasibility and complexity, it's quite hard to locate the issue effectively.

In such difficulties, `tt` comes into play.

With the help of `tt` (_TimeTunnel_), you can check the contexts of the methods at different times in execution history.

## Precautions

- The implementation of the tt command is to save the input parameters/return values of the function into a `Map<Integer, TimeFragment>`. The default size is 100.
- After using tt related functions, you need to manually release the memory, otherwise OOM may occur for a long time. Exiting arthas will not automatically clear tt's cache map.

## Usage

### Start Demo

Start `math-game` in [Quick Start](quick-start.md).

### Record method calls

```bash
$ tt -t demo.MathGame primeFactors
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 66 ms.
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-12-04 11:15:38  1.096236  false   true     0x4b67cf4d     MathGame                       primeFactors
 1001    2018-12-04 11:15:39  0.191848  false   true     0x4b67cf4d     MathGame                       primeFactors
 1002    2018-12-04 11:15:40  0.069523  false   true     0x4b67cf4d     MathGame                       primeFactors
 1003    2018-12-04 11:15:41  0.186073  false   true     0x4b67cf4d     MathGame                       primeFactors
 1004    2018-12-04 11:15:42  17.76437  true    false    0x4b67cf4d     MathGame                       primeFactors
```

### Specify the max number of matched Classes

```bash
$ tt -t -m 1 demo.MathGame primeFactors
Press Q or Ctrl+C to abort.
Affect(class count:1 , method count:1) cost in 130 ms, listenerId: 1.
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2022-12-25 19:41:45  2.629929  true    false    0x3bf400       MathGame                       primeFactors
 1001    2022-12-25 19:41:55  0.146161  false   true     0x3bf400       MathGame                       primeFactors
```

- `-t`

  record the calling context of the method `demo.MathGame primeFactors`

- `-n 3`

  limit the number of the records (avoid overflow for too many records; with `-n` option, Arthas can automatically stop recording once the records reach the specified limit)

- `-m 1`

  limit the number of matched Classes to avoid JVM suspending when too many matched Classes. The default value is 50.

- Property

| Name      | Specification                                  |
| --------- | ---------------------------------------------- |
| INDEX     | the index for each call based on time          |
| TIMESTAMP | time to invoke the method                      |
| COST(ms)  | time cost of the method call                   |
| IS-RET    | whether method exits with normal return        |
| IS-EXP    | whether method failed with exceptions          |
| OBJECT    | `hashCode()` of the object invoking the method |
| CLASS     | class name of the object invoking the method   |
| METHOD    | method being invoked                           |

- Condition expression

Tips:

1. `tt -t *Test print params.length==1` with different amounts of parameters;
2. `tt -t *Test print 'params[1] instanceof Integer'` with different types of parameters;
3. `tt -t *Test print params[0].mobile=="13989838402"` with specified parameter.

Advanced:

- [Critical fields in expression](advice-class.md)
- [Special usage](https://github.com/alibaba/arthas/issues/71)
- [OGNL official guide](https://commons.apache.org/dormant/commons-ognl/language-guide.html)

### List all records

```bash
$ tt -l
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-12-04 11:15:38  1.096236  false   true     0x4b67cf4d     MathGame                       primeFactors
 1001    2018-12-04 11:15:39  0.191848  false   true     0x4b67cf4d     MathGame                       primeFactors
 1002    2018-12-04 11:15:40  0.069523  false   true     0x4b67cf4d     MathGame                       primeFactors
 1003    2018-12-04 11:15:41  0.186073  false   true     0x4b67cf4d     MathGame                       primeFactors
 1004    2018-12-04 11:15:42  17.76437  true    false    0x4b67cf4d     MathGame                       primeFactors
                              9
 1005    2018-12-04 11:15:43  0.4776    false   true     0x4b67cf4d     MathGame                       primeFactors
Affect(row-cnt:6) cost in 4 ms.
```

### Searching for records

```bash
$ tt -s 'method.name=="primeFactors"'
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-12-04 11:15:38  1.096236  false   true     0x4b67cf4d     MathGame                       primeFactors
 1001    2018-12-04 11:15:39  0.191848  false   true     0x4b67cf4d     MathGame                       primeFactors
 1002    2018-12-04 11:15:40  0.069523  false   true     0x4b67cf4d     MathGame                       primeFactors
 1003    2018-12-04 11:15:41  0.186073  false   true     0x4b67cf4d     MathGame                       primeFactors
 1004    2018-12-04 11:15:42  17.76437  true    false    0x4b67cf4d     MathGame                       primeFactors
                              9
 1005    2018-12-04 11:15:43  0.4776    false   true     0x4b67cf4d     MathGame                       primeFactors
Affect(row-cnt:6) cost in 607 ms.
```

Advanced:

- [Critical fields in expression](advice-class.md)

### Check context of the call

Using `tt -i <index>` to check a specific calling details.

```bash
$ tt -i 1003
 INDEX            1003
 GMT-CREATE       2018-12-04 11:15:41
 COST(ms)         0.186073
 OBJECT           0x4b67cf4d
 CLASS            demo.MathGame
 METHOD           primeFactors
 IS-RETURN        false
 IS-EXCEPTION     true
 PARAMETERS[0]    @Integer[-564322413]
 THROW-EXCEPTION  java.lang.IllegalArgumentException: number is: -564322413, need >= 2
                    at demo.MathGame.primeFactors(MathGame.java:46)
                    at demo.MathGame.run(MathGame.java:24)
                    at demo.MathGame.main(MathGame.java:16)

Affect(row-cnt:1) cost in 11 ms.
```

### Replay record

Since Arthas stores the context of the call, you can even _replay_ the method calling afterwards with extra option `-p` to replay the issue for advanced troubleshooting, option `--replay-times`
define the replay execution times, option `--replay-interval` define the interval(unit in ms,with default value 1000) of replays

```bash
$ tt -i 1004 -p
 RE-INDEX       1004
 GMT-REPLAY     2018-12-04 11:26:00
 OBJECT         0x4b67cf4d
 CLASS          demo.MathGame
 METHOD         primeFactors
 PARAMETERS[0]  @Integer[946738738]
 IS-RETURN      true
 IS-EXCEPTION   false
 RETURN-OBJ     @ArrayList[
                    @Integer[2],
                    @Integer[11],
                    @Integer[17],
                    @Integer[2531387],
                ]
Time fragment[1004] successfully replayed.
Affect(row-cnt:1) cost in 14 ms.
```

### Watch express

`-w, --watch-express` watch the time fragment by ognl express.

- You can used all variables in [fundamental fields in expressions](advice-class.md) for the watch express。

```bash
[arthas@10718]$ tt -t demo.MathGame run -n 5
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 56 ms, listenerId: 1
 INDEX      TIMESTAMP                   COST(ms)     IS-RET     IS-EXP      OBJECT              CLASS                                     METHOD
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 1000       2021-01-08 21:54:17         0.901091     true       false       0x7699a589          MathGame                                  run
[arthas@10718]$ tt -w 'target.illegalArgumentCount'  -x 1 -i 1000
@Integer[60]
Affect(row-cnt:1) cost in 7 ms.
```

- Get a static field and calling a static method

```bash
[arthas@10718]$ tt -t demo.MathGame run -n 5
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 56 ms, listenerId: 1
 INDEX      TIMESTAMP                   COST(ms)     IS-RET     IS-EXP      OBJECT              CLASS                                     METHOD
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 1000       2021-01-08 21:54:17         0.901091     true       false       0x7699a589          MathGame                                  run
[arthas@10718]$ tt -w '@demo.MathGame@random.nextInt(100)'  -x 1 -i 1000
@Integer[46]
```

Note that `com.taobao.arthas.core.advisor.Advice#getLoader` is used here, and that it is better to use the exact `classloader` [ognl](ognl.md).

Advanced usage [get spring context to call the bean method](https://github.com/alibaba/arthas/issues/482)

F.Y.I

1. **Loss** of the `ThreadLocal`

   Arthas save params into an array, then invoke the method with the params again. The method execute in another thread, so the `ThreadLocal` **lost**.

2. params may be modified

   Arthas save params into an array, they are object references. The Objects may be modified by other code.

### Delete the specified tt record by index

```
tt -d 1001
```

### Clear all tt records

```
tt --delete-all
```
