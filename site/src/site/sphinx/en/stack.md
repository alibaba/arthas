stack
=====

> Print out the full call stack of the current method.

Most often we know one method gets called, but we have no idea on which code path gets executed or when the method gets called since there are so many code paths to the target method. The command `stack` comes to rescue in this difficult situation.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-expression*|condition expression|
|`[E]`|turn on regex match, the default behavior is wildcard match|
|`[n:]`|execution times|

There's one thing worthy noting here is observation expression. The observation expression supports OGNL grammar, for example, you can come up a expression like this `"{params,returnObj}"`. All OGNL expressions are supported as long as they are legal to the grammar.

Thanks for `advice`'s data structure, it is possible to observe from varieties of different angles. Inside `advice` parameter, all necessary information for notification can be found.

Pls. refer to [core parameters in expression](advice-class.md) for more details.
* Pls. also refer to [https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71) for more advanced usage
* OGNL official site: [https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

> Notes: if there's quotes character (say, `'`) in the expression, then the whole expression must be wrapped by quotes but with the other type (in this case, `"`) too. On contrary, it's no need to quote the expression itself if there's no quotes character found in it, but it is strongly recommended.

Print out calling stack when the first method parameter is "K9-BD-01" for 'getProductById()' method:

```bash
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
...
```

Print out the calling stack when method 'execute()' takes more than 30ms to finish:

```bash
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
...
```
