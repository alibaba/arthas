stack
=====

Print out the full call stack trace containing the current method.

Most of the time, we know the method being invoked but not always we know **HOW being invoked**; `stack` can be a great help to locate the *source* for you. 

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-expression*|condition expression|
|[E]|turn on regex matching while the default is wildcard matching|
|[n:]|calling times|

F.Y.I
1. any valid OGNL expression as `"{params,returnObj}"` supported;
2. filter by time cost as `trace *StringUtils isBlank '#cost>100'`; calling stack with only time cost higher than `100ms` will be printed.

Attention:
1. `#cost` can be used in `watch/stack/trace`;
2. using `#cost` in Arthas 3.0 instead of `$cost`.


Advanced:
* [Critical fields in expression](advice-class.md)
* [Special usage](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)


### Usage

The quoting rules: if there are quotes within the expression, use another type of quotes to quote the whole expression (single `''` or double `""` quotes). 

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
...
```

Filtering by time cost:

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
...
```
