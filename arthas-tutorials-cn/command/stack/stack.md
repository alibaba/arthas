> 输出当前方法被调用的调用路径

很多时候我们都知道一个方法被执行，但这个方法被执行的路径非常多，或者你根本就不知道这个方法是从那里被执行了，此时你需要的是 stack 命令。

### 参数说明

|            参数名称 | 参数说明                             |
| ------------------: | :----------------------------------- |
|     _class-pattern_ | 类名表达式匹配                       |
|    _method-pattern_ | 方法名表达式匹配                     |
| _condition-express_ | 条件表达式                           |
|                 [E] | 开启正则表达式匹配，默认为通配符匹配 |
|              `[n:]` | 执行次数限制                         |

[stack 命令文档](https://arthas.aliyun.com/doc/stack.html)

这里重点要说明的是观察表达式，观察表达式的构成主要由 ognl 表达式组成，所以你可以这样写`"{params,returnObj}"`，只要是一个合法的 ognl 表达式，都能被正常支持。

观察的维度也比较多，主要体现在参数 `advice` 的数据结构上。`Advice` 参数最主要是封装了通知节点的所有信息。

- 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
- OGNL 表达式官网：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### 使用例子

#### stack

`stack demo.MathGame primeFactors`{{execute T2}}

按 `Q`{{exec interrupt}} 或者 `Ctrl+c`{{exec interrupt}} 退出

#### 据条件表达式来过滤

`stack demo.MathGame primeFactors 'params[0]<0' -n 2`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出

#### 据执行时间来过滤

`stack demo.MathGame primeFactors '#cost>5'`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出
