Monitor methods in data aspect including `return values`, `exceptions` and `parameters`.

With the help of [OGNL](https://commons.apache.org/proper/commons-ognl/index.html), you can easily check the details of variables when methods being invoked.

### Parameters & Options

There are four different scenarios for [watch command](https://arthas.aliyun.com/en/doc/watch.html), which makes it rather complicated.

|                   Name | Specification                                                    |
| ---------------------: | :--------------------------------------------------------------- |
|        _class-pattern_ | pattern for the class name                                       |
|       _method-pattern_ | pattern for the method name                                      |
|           _expression_ | expression to watch, default value `{params, target, returnObj}` |
| _condition-expression_ | condition expression to filter                                   |
|                    [b] | before method being invoked                                      |
|                    [e] | when method encountering exceptions                              |
|                    [s] | when method exits normally                                       |
|                    [f] | when method exits (either succeed or fail with exceptions)       |
|                    [E] | turn on regex matching while the default is wildcard matching    |
|                   [x:] | the depth to print the specified property with default value: 1  |

F.Y.I

1. any valid OGNL expression as `"{params,returnObj}"` supported
2. there are four _watching_ points: `-b`, `-e`, `-s` and `-f` (the first three are off in default while `-f` on);
3. at the _watching_ point, Arthas will use the _expression_ to evaluate the variables and print them out;
4. `in parameters` and `out parameters` are different since they can be modified within the invoked methods; `params` stands for `in parameters` in `-b`while `out parameters` in other _watching_ points;
5. there are no `return values` and `exceptions` when using `-b`.
6. In the result of the watch command, the `location` information will be printed. There are three possible values for `location`: `AtEnter`, `AtExit`, and `AtExceptionExit`. Corresponding to the method entry, the method returns normally, and the method throws an exception.

Advanced:

- [Special usages](https://github.com/alibaba/arthas/issues/71)
- [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### Check the `out parameters`, `this` and `return value`

> The expression to watch, default value `{params, target, returnObj}`

- In the above result, the method is executed twice, the first result is `location=AtExceptionExit`, indicating that the method throws an exception, so `returnObj` is null
- In the second result is `location=AtExit`, indicating that the method returns normally, so you can see that the result of `returnObj` is an ArrayList

#### Check `in parameters`

`watch demo.MathGame primeFactors "{params,returnObj}" -x 2 -b`{{execute T2}}

Press `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}} to abort

Compared to the previous _check_:

- `return value` is `null` since it's `-b`.

#### Check _before_ and _after_ at the same time

`watch demo.MathGame primeFactors "{params,target,returnObj}" -x 2 -b -s -n 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

F.Y.I

- `-n 2`: threshold of execution times is 2.
- the first block of output is the _before watching_ point;
- *the order of the output determined by the *watching\* order itself (nothing to do with the order of the options `-b -s`).

#### Use `-x` to check more details

`watch demo.MathGame primeFactors "{params,target}" -x 3`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

- `-x`: Expand level of object (1 by default)

#### Use condition expressions to locate specific call

`watch demo.MathGame primeFactors "{params[0],target}" "params[0]<0"`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

- Only calls that meet the conditions will respond.

- `Watch Express` single value can not be added '{}', and multiple values need to be added '{a, B, C}'.

- `condition Express` cannot add '{}', you can use commas to separate subexpressions and take the last value of the expression to judge.

If there are other overloaded methods with the same name in the watch method, you can filter them by the following methods:

- Filter according to parameter type

`watch demo.MathGame primeFactors '{params, params[0].class.name}' 'params[0].class.name == "java.lang.Integer"'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

- Filter according to the number of parameters

`watch demo.MathGame primeFactors '{params, params.length}' 'params.length==1'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

#### Check `exceptions`

`watch demo.MathGame primeFactors "{params[0],throwExp}" -e -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

- `-e`: Trigger when an exception is thrown
- `throwExp`: the exception object

Filter according to exception type or message:

`watch demo.MathGame primeFactors '{params, throwExp}' '#msg=throwExp.toString(), #msg.contains("IllegalArgumentException")' -e -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

#### Filter by time cost

`watch demo.MathGame primeFactors '{params, returnObj}' '#cost>200' -x 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

- `#cost>200` (`ms`) filter out all invokings that take less than `200ms`.

#### Check the field of the target object

- `target` is the `this` object in java.

`watch demo.MathGame primeFactors 'target'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

- `target.field_name`: the field of the current object.

`watch demo.MathGame primeFactors 'target.illegalArgumentCount'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort
