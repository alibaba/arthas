trace
=====

Track the `class-pattern` & `method-pattern` matched method calling trace and print the time cost in each call.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-express*|condition expression|
|[E]|turn on regx matching while the default is wildcards matching|
|`[n:]`|calling times|
|`#cost`|time cost|

F.Y.I
1. any valid OGNL expression as `"{params,returnObj}"` supported;
2. filter by time cost as `trace *StringUtils isBlank '$cost>100'`; calling stack with only time cost higher than `100ms` will be printed.

Attention:
1. `$cost` can be used in `watch/stack/trace`;
2. using `#cost` in Arthas 3.0 instead of `$cost`.
3. `trace` can help to locate the performance lurking issue but only `level-one` method invoking considered.


Advanced:
* [Critical fields in expression](advice-class.md)
* [Special usage](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

A demo:

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

Tracing down method `add`:

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

Filtering by time cost:

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

Only the calling trace of the time cost higher than `4ms`presented now.

F.Y.I
1. just like JProfile and the like commercial software, you can `trace` down the specified method calling in Arthas;
2. there will be some overhead using `trace` but not much;
3. the time cost is an instructive clue for troubleshooting, which means it's not that accurate ignoring the cost it itself causes; the deeper or more the call is, the accuracy is becoming worse;
4. `[0,0,0ms,11]xxx:yyy() [throws Exception]`，the same method calling aggregated into one line here while `throws Exception` indicates there is an exception.

