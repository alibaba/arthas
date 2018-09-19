trace
===

> 方法内部调用路径，并输出方法路径上的每个节点上耗时

`trace` 命令能主动搜索 `class-pattern`／`method-pattern` 对应的方法调用路径，渲染和统计整个调用链路上的所有性能开销和追踪调用链路。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|*condition-express*|条件表达式|
|[E]|开启正则表达式匹配，默认为通配符匹配|
|`[n:]`|命令执行次数|
|`#cost`|方法执行耗时|

这里重点要说明的是观察表达式，观察表达式的构成主要由 ognl 表达式组成，所以你可以这样写`"{params,returnObj}"`，只要是一个合法的 ognl 表达式，都能被正常支持。

观察的维度也比较多，主要体现在参数 `advice` 的数据结构上。`Advice` 参数最主要是封装了通知节点的所有信息。


请参考[表达式核心变量](advice-class.md)中关于该节点的描述。

* 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
* OGNL表达式官网：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

很多时候我们只想看到某个方法的rt大于某个时间之后的trace结果，现在Arthas可以按照方法执行的耗时来进行过滤了，例如`trace *StringUtils isBlank '$cost>100'`表示当执行时间超过100ms的时候，才会输出trace的结果。

> 注意：
> 1. watch/stack/trace这个三个命令都支持$cost
> 2. 如果是Arthas 3.0，请把`$cost`改为`#cost`

### 注意事项

`trace` 能方便的帮助你定位和发现因 RT 高而导致的性能问题缺陷，但其每次只能跟踪一级方法的调用链路。

### 使用参考

代码示例：

```java
    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");

        List<String> list2 = new ArrayList<String>();
        list2.add("c");
        list2.add("d");

        int len = add(list, list2);
    }

    private static int add(List<String> list, List<String> list2) {
        int i = 10;
        while (i >= 0) {
            try {
                hehe(i);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            i--;
        }

        list.addAll(list2);
        return list.size();
    }

    private static void hehe(int i) {
        if (i == 0) {
            throw new RuntimeException("ZERO");
        }
    }
```

监测 `add` 方法：

```shell
$ trace com.alibaba.sample.petstore.web.store.module.screen.ItemList add params.length==2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 144 ms.
`---Tracing...
    `---[2ms]com.alibaba.sample.petstore.web.store.module.screen.ItemList:add()
        +---[0,0,0ms,11]com.alibaba.sample.petstore.web.store.module.screen.ItemList:hehe() [throws Exception]
        +---[1ms]java.lang.Throwable:printStackTrace()
        +---[0ms]java.util.List:addAll()
        `---[0ms]java.util.List:size()
```

按照耗时过滤：

```shell
$ trace com.alibaba.sample.petstore.web.store.module.screen.ItemList execute #cost>4
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 159 ms.
trace com.alibaba.sample.petstore.web.store.module.screen.ItemList execute #cost>4
`---thread_name=http-nio-8080-exec-5;id=2c;is_daemon=true;priority=5;TCCL=com.taobao.pandora.boot.embedded.tomcat.TomcatEmbeddedWebappClassLoader
    `---[8.866586ms] com.alibaba.sample.petstore.web.store.module.screen.ItemList:execute()
        +---[2.847106ms] com.alibaba.sample.petstore.biz.StoreManager:getAllProductItems()
        +---[0.765544ms] com.alibaba.sample.petstore.dal.dao.ProductDao:getProductById()
        +---[0.021204ms] com.alibaba.sample.petstore.dal.dataobject.Product:getCategoryId()
        +---[1.341532ms] com.alibaba.sample.petstore.dal.dao.CategoryDao:getCategoryById()
        `---[min=0.005428ms,max=0.094064ms,total=0.105228ms,count=3] com.alibaba.citrus.turbine.Context:put()
```
> 只会展示耗时大于4ms的调用路径，有助于在排查问题的时候，只关注异常情况

- 是不是很眼熟，没错，在 JProfiler 等收费软件中你曾经见识类似的功能，这里你将可以通过命令就能打印出指定调用路径。 友情提醒下，`trace` 在执行的过程中本身是会有一定的性能开销，在统计的报告中并未像 JProfiler 一样预先减去其自身的统计开销。所以这统计出来有些许的不准，渲染路径上调用的类、方法越多，性能偏差越大。但还是能让你看清一些事情的。
- [2ms] 的含义，`2` 的含义是：当前节点在当前步骤的耗时，单位为毫秒
- [0,0,0ms,11]xxx:yyy() [throws Exception]，对该方法中相同的方法调用进行了合并，`0,0,0ms,11` 表示方法调用耗时，`min,max,total,count`；`throws Exception` 表明该方法调用中存在异常返回
- 这里存在一个统计不准确的问题，就是所有方法耗时加起来可能会小于该监测方法的总耗时，这个是由于 Arthas 本身的逻辑会有一定的耗时