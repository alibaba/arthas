trace
=====

> Trace method calling path, and output the time cost for each node in the path.

`trace` can track the calling path specified by `class-pattern` / `method-pattern`, and calculate the time cost on the whole path.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-express*|condition expression|
|`[E]`|enable regex match, the default behavior is wildcards match|
|`[n:]`|execution times|
|#cost|time cost|

There's one thing worthy noting here is observation expression. The observation expression supports OGNL grammar, for example, you can come up a expression like this `"{params,returnObj}"`. All OGNL expressions are supported as long as they are legal to the grammar.

Thanks for `advice`'s data structure, it is possible to observe from varieties of different angles. Inside `advice` parameter, all necessary information for notification can be found.

Pls. refer to [core parameters in expression](advice-class.md) for more details.
* Pls. also refer to [https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71) for more advanced usage
* OGNL official site: [https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

Many times what we are interested is the exact trace result when the method call takes time over one particular period. It is possible to achieve this in Arthas, for example: `trace *StringUtils isBlank '$cost>100'` means trace result will only be output when the executing time exceeds 100ms.

> Notes:
> 1. `watch`/`stack`/`trace`, these three commands all support `$cost`.
> 2. On version `3.0`, pls. use `#cost` instead of `$cost`.

### Notice

`trace` is handy to help discovering and locating the performance flaws in your system, but pls. note Arthas can only trace the first level method call each time.

### Usage

Sample code:

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

Trace down method `add`:

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

Filter by time cost:

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

> Only the call path which's time cost is higher than `4ms` will be shown. This feature is handy to focus on what's needed to focus when troubleshoot.

* Here Arthas provides the similar functionality JProfile and other commercial software provide. Compared to these professional softwares, Arthas doesn't deduce the time cost `trace` itself takes, therefore it is not as accurate as these softwares offer. More classes and methods on the calling path, more inaccurate `trace` output is, but it is still helpful for diagnostics where the bottleneck is.
* "[2.847106ms] com.alibaba.sample.petstore.biz.StoreManager:getAllProductItems()" means "getAllProductItem()" method from "com.alibaba.sample.petstore.biz.StoreManager" takes `2.847106` ms.
* "[min=0.005428ms,max=0.094064ms,total=0.105228ms,count=3] com.alibaba.citrus.turbine.Context:put()" means aggregating all same method calls into one single line. The minimum time cost is `0.005428` ms, the maximum time cost is `0.094064` ms, and the total time cost for all method calls (`3` times in total) to "com.alibaba.citrus.turbine.Context:put()" is `0.105228ms`. If "throws Exception" appears in this line, it means some exceptions have been thrown from this method calls.
* The total time cost may not equal to the sum of the time costs each sub method call takes, this is because Arthas instrumented code takes time too.

