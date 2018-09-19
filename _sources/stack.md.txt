stack
===

> 输出当前方法被调用的调用路径

很多时候我们都知道一个方法被执行，但这个方法被执行的路径非常多，或者你根本就不知道这个方法是从那里被执行了，此时你需要的是 stack 命令。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|*condition-express*|条件表达式|
|[E]|开启正则表达式匹配，默认为通配符匹配|
|`[n:]`|执行次数限制|

这里重点要说明的是观察表达式，观察表达式的构成主要由 ognl 表达式组成，所以你可以这样写`"{params,returnObj}"`，只要是一个合法的 ognl 表达式，都能被正常支持。

观察的维度也比较多，主要体现在参数 `advice` 的数据结构上。`Advice` 参数最主要是封装了通知节点的所有信息。


请参考[表达式核心变量](advice-class.md)中关于该节点的描述。

* 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
* OGNL表达式官网：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### 使用例子

> 注意：如果表达式里面包含了引号，那么需要把整个表达式用引号括起来，如果表达式中没有包含引号，那么可以不用引号。当然，一个好的习惯是，不管表达式中有没有引号，都使用引号括起来。

```
$ stack com.alibaba.sample.petstore.dal.dao.ProductDao getProductById 'params[0]=="K9-BD-01"'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 51 ms.
thread_name="http-bio-8080-exec-4" thread_id=0x4a;is_daemon=true;priority=5;
    @com.alibaba.sample.petstore.dal.dao.ibatis.IbatisProductDao.getProductById()
        at com.alibaba.sample.petstore.web.store.module.screen.ItemList.execute(ItemList.java:50)
        at com.alibaba.sample.petstore.web.store.module.screen.ItemList$$FastClassByCGLIB$$40b2f45f.invoke(<generated>:-1)
        at net.sf.cglib.reflect.FastMethod.invoke(FastMethod.java:53)
        at com.alibaba.citrus.service.moduleloader.impl.adapter.MethodInvoker.invoke(MethodInvoker.java:70)
        at com.alibaba.citrus.service.moduleloader.impl.adapter.DataBindingAdapter.executeAndReturn(DataBindingAdapter.java:41)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformScreenValve.performScreenModule(PerformScreenValve.java:111)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformScreenValve.invoke(PerformScreenValve.java:74)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformActionValve.invoke(PerformActionValve.java:73)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invoke(PipelineImpl.java:210)
......

thread_name="http-bio-8080-exec-2" thread_id=0x48;is_daemon=true;priority=5;
    @com.alibaba.sample.petstore.dal.dao.ibatis.IbatisProductDao.getProductById()
        at com.alibaba.sample.petstore.web.store.module.screen.ItemList.execute(ItemList.java:50)
        at com.alibaba.sample.petstore.web.store.module.screen.ItemList$$FastClassByCGLIB$$40b2f45f.invoke(<generated>:-1)
        at net.sf.cglib.reflect.FastMethod.invoke(FastMethod.java:53)
        at com.alibaba.citrus.service.moduleloader.impl.adapter.MethodInvoker.invoke(MethodInvoker.java:70)
        at com.alibaba.citrus.service.moduleloader.impl.adapter.DataBindingAdapter.executeAndReturn(DataBindingAdapter.java:41)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformScreenValve.performScreenModule(PerformScreenValve.java:111)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformScreenValve.invoke(PerformScreenValve.java:74)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformActionValve.invoke(PerformActionValve.java:73)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invoke(PipelineImpl.java:210)
        at com.alibaba.citrus.service.pipeline.impl.valve.ChooseValve.invoke(ChooseValve.java:98)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
......
```

按照耗时查询: 

```
$ stack com.alibaba.sample.petstore.web.store.module.screen.ItemList execute #cost>30
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 123 ms.
stack com.alibaba.sample.petstore.web.store.module.screen.ItemList execute #cost>30
thread_name=http-nio-8080-exec-10;id=31;is_daemon=true;priority=5;TCCL=com.taobao.pandora.boot.embedded.tomcat.TomcatEmbeddedWebappClassLoader
    @com.alibaba.sample.petstore.web.store.module.screen.ItemList.execute()
        at com.alibaba.sample.petstore.web.store.module.screen.ItemList$$FastClassByCGLIB$$40b2f45f.invoke(<generated>:-1)
        at net.sf.cglib.reflect.FastMethod.invoke(FastMethod.java:53)
        at com.alibaba.citrus.service.moduleloader.impl.adapter.MethodInvoker.invoke(MethodInvoker.java:70)
        at com.alibaba.citrus.service.moduleloader.impl.adapter.DataBindingAdapter.executeAndReturn(DataBindingAdapter.java:41)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformScreenValve.performScreenModule(PerformScreenValve.java:111)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformScreenValve.invoke(PerformScreenValve.java:74)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.PerformActionValve.invoke(PerformActionValve.java:73)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invoke(PipelineImpl.java:210)
        at com.alibaba.citrus.service.pipeline.impl.valve.ChooseValve.invoke(ChooseValve.java:98)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invoke(PipelineImpl.java:210)
        at com.alibaba.citrus.service.pipeline.impl.valve.LoopValve.invokeBody(LoopValve.java:105)
        at com.alibaba.citrus.service.pipeline.impl.valve.LoopValve.invoke(LoopValve.java:83)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.PageAuthorizationValve.invoke(PageAuthorizationValve.java:105)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.CheckCsrfTokenValve.invoke(CheckCsrfTokenValve.java:123)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.AnalyzeURLValve.invoke(AnalyzeURLValve.java:126)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.SetLoggingContextValve.invoke(SetLoggingContextValve.java:66)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.turbine.pipeline.valve.PrepareForTurbineValve.invoke(PrepareForTurbineValve.java:52)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invokeNext(PipelineImpl.java:157)
        at com.alibaba.citrus.service.pipeline.impl.PipelineImpl$PipelineContextImpl.invoke(PipelineImpl.java:210)
        at com.alibaba.citrus.webx.impl.WebxControllerImpl.service(WebxControllerImpl.java:43)
        at com.alibaba.citrus.webx.impl.WebxRootControllerImpl.handleRequest(WebxRootControllerImpl.java:53)
        at com.alibaba.citrus.webx.support.AbstractWebxRootController.service(AbstractWebxRootController.java:165)
.........   
```
>只会打印出耗时超过30ms的堆栈情况