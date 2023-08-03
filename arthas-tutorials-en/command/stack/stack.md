> Print out the full call stack of the current method.

Most often we know one method gets called, but we have no idea on which code path gets executed or when the method gets called since there are so many code paths to the target method. The command `stack` comes to rescue in this difficult situation.

### Parameters

|                   Name | Specification                                               |
| ---------------------: | :---------------------------------------------------------- |
|        _class-pattern_ | pattern for the class name                                  |
|       _method-pattern_ | pattern for the method name                                 |
| _condition-expression_ | condition expression                                        |
|                  `[E]` | turn on regex match, the default behavior is wildcard match |
|                 `[n:]` | execution times                                             |

[stack command Docs](https://arthas.aliyun.com/en/doc/stack.html)

There's one thing worthy noting here is observation expression. The observation expression supports OGNL grammar, for example, you can come up a expression like this `"{params,returnObj}"`. All OGNL expressions are supported as long as they are legal to the grammar.

Thanks for `advice`'s data structure, it is possible to observe from varieties of different angles. Inside `advice` parameter, all necessary information for notification can be found.

- Pls. also refer to [https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71) for more advanced usage
- OGNL official site: [https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### stack

`stack demo.MathGame primeFactors`{{execute T2}}

Press `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}} to abort

#### Filtering by condition expression

`stack demo.MathGame primeFactors 'params[0]<0' -n 2`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

#### Filtering by cost

`stack demo.MathGame primeFactors '#cost>5'`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort
