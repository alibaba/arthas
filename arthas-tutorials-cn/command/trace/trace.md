> 方法内部调用路径，并输出方法路径上的每个节点上耗时

`trace` 命令能主动搜索 `class-pattern`／`method-pattern` 对应的方法调用路径，渲染和统计整个调用链路上的所有性能开销和追踪调用链路。

### 参数说明

|            参数名称 | 参数说明                             |
| ------------------: | :----------------------------------- |
|     _class-pattern_ | 类名表达式匹配                       |
|    _method-pattern_ | 方法名表达式匹配                     |
| _condition-express_ | 条件表达式                           |
|                 [E] | 开启正则表达式匹配，默认为通配符匹配 |
|              `[n:]` | 命令执行次数                         |
|             `#cost` | 方法执行耗时                         |

[trace 命令文档](https://arthas.aliyun.com/doc/trace.html)

这里重点要说明的是观察表达式，观察表达式的构成主要由 ognl 表达式组成，所以你可以这样写`"{params,returnObj}"`，只要是一个合法的 ognl 表达式，都能被正常支持。

观察的维度也比较多，主要体现在参数 `advice` 的数据结构上。`Advice` 参数最主要是封装了通知节点的所有信息。

请参考[表达式核心变量](advice-class.md)中关于该节点的描述。

- 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
- OGNL 表达式官网：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

很多时候我们只想看到某个方法的 rt 大于某个时间之后的 trace 结果，现在 Arthas 可以按照方法执行的耗时来进行过滤了，例如`trace *StringUtils isBlank '#cost>100'`表示当执行时间超过 100ms 的时候，才会输出 trace 的结果。

> watch/stack/trace 这个三个命令都支持`#cost`

### 注意事项

`trace` 能方便的帮助你定位和发现因 RT 高而导致的性能问题缺陷，但其每次只能跟踪一级方法的调用链路。

参考：[Trace 命令的实现原理](https://github.com/alibaba/arthas/issues/597)

3.3.0 版本后，可以使用动态 Trace 功能，不断增加新的匹配类，参考下面的示例。

### 使用参考

#### trace 函数

`trace demo.MathGame run`{{execute T2}}

按 `Q`{{exec interrupt}} 或者 `Ctrl+c`{{exec interrupt}} 退出

#### trace 次数限制

如果方法调用的次数很多，那么可以用`-n`参数指定捕捉结果的次数。比如下面的例子里，捕捉到一次调用就退出命令。

`trace demo.MathGame run -n 1`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出

#### 包含 jdk 的函数

- `--skipJDKMethod <value> ` skip jdk method trace, default value true.

`trace --skipJDKMethod false demo.MathGame run`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出

默认情况下，trace 不会包含 jdk 里的函数调用，如果希望 trace jdk 里的函数，需要显式设置`--skipJDKMethod false`。

#### 据调用耗时过滤

`trace demo.MathGame run '#cost > 10'`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出

> 只会展示耗时大于 10ms 的调用路径，有助于在排查问题的时候，只关注异常情况

- 是不是很眼熟，没错，在 JProfiler 等收费软件中你曾经见识类似的功能，这里你将可以通过命令就能打印出指定调用路径。友情提醒下，`trace` 在执行的过程中本身是会有一定的性能开销，在统计的报告中并未像 JProfiler 一样预先减去其自身的统计开销。所以这统计出来有些许的不准，渲染路径上调用的类、方法越多，性能偏差越大。但还是能让你看清一些事情的。
- [12.033735ms] 的含义，`12.033735` 的含义是：当前节点在当前步骤的耗时，单位为毫秒
- [0,0,0ms,11]xxx:yyy() [throws Exception]，对该方法中相同的方法调用进行了合并，`0,0,0ms,11` 表示方法调用耗时，`min,max,total,count`；`throws Exception` 表明该方法调用中存在异常返回
- 这里存在一个统计不准确的问题，就是所有方法耗时加起来可能会小于该监测方法的总耗时，这个是由于 Arthas 本身的逻辑会有一定的耗时

#### trace 多个类或者多个函数

trace 命令只会 trace 匹配到的函数里的子调用，并不会向下 trace 多层。因为 trace 是代价比较贵的，多层 trace 可能会导致最终要 trace 的类和函数非常多。

可以用正则表匹配路径上的多个类和函数，一定程度上达到多层 trace 的效果。

```bash
trace -E com.test.ClassA|org.test.ClassB method1|method2|method3
```

### 动态 trace

3.3.0 版本后支持。

打开终端 1，trace `run`函数，可以看到打印出 `listenerId: 1`：

`trace demo.MathGame run`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出

现在想要深入子函数`primeFactors`，可以打开一个新终端 2，使用`telnet localhost 3658`连接上 arthas，再 trace `primeFactors`时，指定`listenerId`。

`trace demo.MathGame primeFactors --listenerId 1`{{execute T2}}

按`Q`{{execute T2}}或者`Ctrl+c`退出

这时终端 2 打印的结果，说明已经增强了一个函数：`Affect(class count: 1 , method count: 1)`，但不再打印更多的结果。

再查看终端 1，可以发现 trace 的结果增加了一层，打印了`primeFactors`函数里的内容：

通过指定`listenerId`的方式动态 trace，可以不断深入。另外 `watch`/`tt`/`monitor`等命令也支持类似的功能。
