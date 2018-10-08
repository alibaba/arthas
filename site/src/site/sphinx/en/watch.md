watch
=====

Monitor methods in data aspect including `return values`, `exceptions` and `parameters`.

With the help of [OGNL](https://en.wikipedia.org/wiki/OGNL), you can easily check the details of variables when methods being invoked.

### Parameters & Options

There are four different scenarios for `watch` command, which makes it rather complicated. 

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*expression*|expression to monitor|
|*condition-expression*|condition expression to filter|
|[b]|before method being invoked|
|[e]|when method encountering exceptions|
|[s]|when method exits normally|
|[f]|when method exits (either succeed or fail with exceptions)|
|[E]|turn on regex matching while the default is wildcard matching|
|[x:]|the depth to print the specified property with default value: 1|

F.Y.I
1. any valid OGNL expression as `"{params,returnObj}"` supported
2. there are four *watching* points: `-b`, `-e`, `-s` and `-f` (the first three are off in default while `-f` on);
3. at the *watching* point, Arthas will use the *expression* to evaluate the variables and print them out;
4. `in parameters` and `out parameters` are different since they can be modified within the invoked methods; `params` stands for `in parameters` in `-b`while `out parameters` in other *watching* points;
5. there are no `return values` and `exceptions` when using `-b`.


Advanced:
* [Critical fields in *expression*](advice-class.md)
* [Special usages](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

A demo:

```java
	public void execute() {
		List<String> list = new ArrayList<String>();
		list.add("a");
		list.add("b");

		List<String> list2 = new ArrayList<String>();
		list2.add("c");
		list2.add("d");

		int len = add(list, list2);
	}

	private static int add(List<String> list, List<String> list2) {
		list.addAll(list2);
		return list.size();
	}
```
#### Check the `out parameters` and `return value`

```shell
$ watch com.alibaba.sample.petstore.web.store.module.screen.ItemList add "{params,returnObj}" -x 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 44 ms.
@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=4],
        @ArrayList[isEmpty=false;size=2],
    ],

    @Integer[4],
]
```

#### Check `in parameters`

```shell
$ watch com.alibaba.sample.petstore.web.store.module.screen.ItemList add "{params,returnObj}" -x 2 -b
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 48 ms.  
@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=2],
        @ArrayList[isEmpty=false;size=2],
    ],

    null,
]
```

Compared to the previous *check*, there are two differences:
1. size of `params[0]` is `2` instead of `4`;
2. `return value` is `null` since it's `-b`.


#### Check *before* and *after* at the same time

```shell
$ watch com.alibaba.sample.petstore.web.store.module.screen.ItemList add "{params,returnObj}" -x 2 -b -s 
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 59 ms.  
@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=2],
        @ArrayList[isEmpty=false;size=2],
    ],

    null,
]
@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=4],
        @ArrayList[isEmpty=false;size=2],
    ],

    @Integer[4],
]
```

F.Y.I
1. the first block of output is the *before watching* point;
2. the order of the output determined by the *watching* order itself (nothing to do with the order of the options `-b -s`).

#### Use `-x` to check more details

```shell
$ watch com.alibaba.sample.petstore.web.store.module.screen.ItemList add "{params,returnObj}" -x 3 
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 59 ms.  
@ArrayList[
    @Object[][
        @ArrayList[
            @String[a],

            @String[b],

            @String[c],

            @String[d],
        ],
        @ArrayList[
            @String[c],

            @String[d],
        ],
    ],

    @Integer[4],
]
```

#### Use condition expressions to locate specific call

```shell
$ watch com.alibaba.sample.petstore.biz.impl.UserManagerImpl testAdd "{params, returnObj}" "params[0].equals('aaa')" -x 2 
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 29 ms.  
@ArrayList[
    @Object[][
        @String[aaa],
        @String[bbb],
    ],

    @Integer[6],
]
```

#### Check `exceptions`

```shell
$ watch com.alibaba.sample.petstore.biz.impl.UserManagerImpl testAdd "{params, throwExp}"  -e -x 2 
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 29 ms.  
@ArrayList[
    @Object[][
        @String[aaa],
        @String[bbb],
    ],

    java.lang.NullPointerException
    	at com.alibaba.sample.petstore.biz.impl.UserManagerImpl.testAdd(UserManagerImpl.java:75)
    	at com.alibaba.sample.petstore.biz.impl.UserManagerImpl.register(UserManagerImpl.java:60)
    	at com.alibaba.sample.petstore.web.user.module.action.RegisterAction.doRegister(RegisterAction.java:45)
    	at com.alibaba.sample.petstore.web.user.module.action.RegisterAction$$FastClassByCGLIB$$ad5428f1.invoke(<generated>)
    	at net.sf.cglib.reflect.FastMethod.invoke(FastMethod.java:53)
    	at com.alibaba.citrus.service.moduleloader.impl.adapter.MethodInvoker.invoke(MethodInvoker.java:70)
    	at com.alibaba.citrus.service.moduleloader.impl.adapter.AbstractModuleEventAdapter.executeAndReturn(AbstractModuleEventAdapter.java:100)
    	at com.alibaba.citrus.service.moduleloader.impl.adapter.AbstractModuleEventAdapter.execute(AbstractModuleEventAdapter.java:58)
    	at com.alibaba.citrus.turbine.pipeline.valve.PerformActionValve.invoke(PerformActionValve.java:63)
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
    	at com.alibaba.citrus.webx.servlet.WebxFrameworkFilter.doFilter(WebxFrameworkFilter.java:152)
    	at com.alibaba.citrus.webx.servlet.FilterBean.doFilter(FilterBean.java:148)
    	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:241)
    	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
    	at com.alibaba.citrus.webx.servlet.SetLoggingContextFilter.doFilter(SetLoggingContextFilter.java:61)
    	at com.alibaba.citrus.webx.servlet.FilterBean.doFilter(FilterBean.java:148)
    	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:241)
    	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
    	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:220)
    	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:122)
    	at com.taobao.tomcat.valves.ContextLoadFilterValve.invoke(ContextLoadFilterValve.java:152)
    	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:170)
    	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:103)
    	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:116)
    	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:429)
    	at org.apache.coyote.http11.AbstractHttp11Processor.process(AbstractHttp11Processor.java:1085)
    	at org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process(AbstractProtocol.java:625)
    	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1760)
    	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.run(NioEndpoint.java:1719)
    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
    	at java.lang.Thread.run(Thread.java:745),
]
```

#### Filter based on time cost

```shell
$ watch com.alibaba.sample.petstore.web.store.module.screen.ItemList add "{params,returnObj}" #cost>200 -x 3 
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 59 ms.  
@ArrayList[
    @Object[][
        @ArrayList[
            @String[a],

            @String[b],

            @String[c],

            @String[d],
        ],
        @ArrayList[
            @String[c],

            @String[d],
        ],
    ],

    @Integer[4],
]
```

F.Y.I
`#cost>200` (`ms`) filter out all invokings that take less than `200ms`.


#### Check the global properties of the target object

`target` stands for the current object.

```
$ watch com.taobao.container.web.arthas.rest.MyAppsController myFavoriteApps 'target'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 128 ms.
ts=2017-10-31 18:45:55;result=@MyAppsController[
    myFavAppsMapper=@$Proxy131[org.apache.ibatis.binding.MapperProxy@563e97f3],
    getAppNameAndIdByEmpId=@$Proxy135[HardCodedTarget(type=GetAppNameAndIdByEmpId, url=http://hello.com)],
    enableWebConsoleAppsMapper=@$Proxy132[org.apache.ibatis.binding.MapperProxy@7d51e4a8],
]
```

`target.field_name` stands for a global property of the current object.

```
$ watch com.taobao.container.web.arthas.rest.MyAppsController myFavoriteApps 'target.myFavAppsMapper'
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 126 ms.
ts=2017-10-31 18:46:17;result=@$Proxy131[
    m1=@Method[public boolean java.lang.Object.equals(java.lang.Object)],
    m2=@Method[public java.lang.String java.lang.Object.toString()],
    m5=@Method[public abstract java.util.List com.taobao.container.dal.arthas.mapper.MyFavAppsMapper.listFavApps(java.util.Map)],
    m3=@Method[public abstract int com.taobao.container.dal.arthas.mapper.MyFavAppsMapper.delete(java.lang.String,java.lang.String,java.lang.String)],
    m4=@Method[public abstract long com.taobao.container.dal.arthas.mapper.MyFavAppsMapper.insert(com.taobao.container.dal.arthas.domain.MyFavAppsDO)],
    m0=@Method[public native int java.lang.Object.hashCode()],
]
``` 
