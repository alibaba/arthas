watch
===

> 方法执行数据观测

让你能方便的观察到指定方法的调用情况。能观察到的范围为：`返回值`、`抛出异常`、`入参`，通过编写 OGNL 表达式进行对应变量的查看。

### 参数说明

watch 的参数比较多，主要是因为它能在 4 个不同的场景观察对象

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|*express*|观察表达式|
|*condition-express*|条件表达式|
|[b]|在**方法调用之前**观察|
|[e]|在**方法异常之后**观察|
|[s]|在**方法返回之后**观察|
|[f]|在**方法结束之后**(正常返回和异常返回)观察|
|[E]|开启正则表达式匹配，默认为通配符匹配|
|[x:]|指定输出结果的属性遍历深度，默认为 1|

这里重点要说明的是观察表达式，观察表达式的构成主要由 ognl 表达式组成，所以你可以这样写`"{params,returnObj}"`，只要是一个合法的 ognl 表达式，都能被正常支持。

观察的维度也比较多，主要体现在参数 `advice` 的数据结构上。`Advice` 参数最主要是封装了通知节点的所有信息。请参考[表达式核心变量](advice-class.md)中关于该节点的描述。

* 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
* OGNL表达式官网：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)

**特别说明**：

* watch 命令定义了4个观察事件点，即 `-b` 方法调用前，`-e` 方法异常后，`-s` 方法返回后和 `-f` 方法结束后
* 4个观察事件点 `-b`、`-e`、`-s` 默认关闭，`-f` 默认打开，当指定观察点被打开后，在相应事件点会对观察表达式进行求值并输出
* 这里要注意`方法入参`和`方法出参`的区别，有可能在中间被修改导致前后不一致，除了 `-b` 事件点 `params` 代表方法入参外，其余事件都代表方法出参
* 当使用 `-b` 时，由于观察事件点是在方法调用前，此时返回值或异常均不存在

### 使用参考

代码示例：

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
#### 观察方法出参和返回值

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

#### 观察方法入参

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

> 对比前一个例子，params[0] 其size为2（入参），返回值为空（事件点为方法执行前，因此获取不到返回值）


#### 同时观察方法调用前和方法返回后

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

>这里输出结果中，第一次输出的是方法调用前的观察表达式的结果，第二次输出的是方法返回后的表达式的结果

>结果的顺序和命令中 `-s -b` 的顺序没有关系，只与事件本身的先后顺序有关  

#### 调整`-x`的值，观察具体的方法参数值

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

>`-x`表示遍历深度，可以调整来打印具体的参数和结果内容。

#### 条件表达式的例子

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

>只有满足条件的调用，才会有响应。

#### 观察异常信息的例子

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

>express中，表示异常信息的变量是`throwExp`

#### 按照耗时进行过滤

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

>#cost>200(单位是`ms`)表示只有当耗时大于200ms时才会输出，过滤掉执行时间小于200ms的调用


#### 观察当前对象中的全局属性

如果想查看方法运行前后，当前对象中的全局属性，可以使用`target`关键字，代表当前对象

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

然后使用`target.field_name`访问当前对象的某个全局属性

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