Check the `parameters`, `return values` and `exceptions` of the methods at different times.

`watch` is a powerful command but due to its feasibility and complexity, it's quite hard to locate the issue effectively.

In such difficulties, [tt command](https://arthas.aliyun.com/en/doc/tt.html) comes into play.

With the help of `tt` (_TimeTunnel_), you can check the contexts of the methods at different times in execution history.

### Usage

#### Record method calls

`tt -t demo.MathGame primeFactors`{{execute T2}}

Press `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}} to abort

- `-t` record the calling context of the method `demo.MathGame primeFactors`

- `-n 3` limit the number of the records (avoid overflow for too many records; with `-n` option, Arthas can automatically stop recording once the records reach the specified limit)

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

- [Critical fields in expression](https://arthas.aliyun.com/doc/en/advice-class.html)
- [Special usage](https://github.com/alibaba/arthas/issues/71)
- [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

#### List all records

`tt -l`{{execute T2}}

#### Searching for records

`tt -s 'method.name=="primeFactors"'`{{execute T2}}

Advanced:

- [Critical fields in expression](advice-class.md)

#### Check context of the call

`tt -i 1003`{{execute T2}}

Using `tt -i <index>` to check a specific calling details.

### Replay record

Since Arthas stores the context of the call, you can even _replay_ the method calling afterwards with extra option `-p` to replay the issue for advanced troubleshooting, option `--replay-times`
define the replay execution times, option `--replay-interval` define the interval(unit in ms,with default value 1000) of replays

`tt -i 1004 -p`{{execute T2}}

F.Y.I

1. **Loss** of the `ThreadLocal`

   Arthas save params into an array, then invoke the method with the params again. The method execute in another thread, so the `ThreadLocal` **lost**.

1. params may be modified

   Arthas save params into an array, they are object references. The Objects may be modified by other code.
