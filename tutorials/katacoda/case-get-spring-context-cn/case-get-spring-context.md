

在这个案例里，展示获取spring context，再获取bean，然后调用函数。

### 使用tt命令获取到spring context

`tt`即 TimeTunnel，它可以记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测。

* https://arthas.aliyun.com/doc/tt.html

`tt -t org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter invokeHandlerMethod`{{execute T2}}

访问：https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/1

可以看到`tt`命令捕获到了一个请求：

```bash
$ tt -t org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdaptePress Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 252 ms.
 INDE  TIMESTAMP    COST(  IS-R  IS-  OBJECT     CLASS               METHOD
 X                  ms)    ET    EXP
-----------------------------------------------------------------------------------------
 1000  2019-02-15   4.583  true  fal  0xc93cf1a  RequestMappingHand  invokeHandlerMethod
       15:38:32     923          se              lerAdapter
```

### 使用tt命令从调用记录里获取到spring context

输入 `Q`{{execute T2}} 或者 `Ctrl + C` 退出上面的 `tt -t`命令。

`tt -i 1000 -w 'target.getApplicationContext()'`{{execute T2}}

```bash
$ tt -i 1000 -w 'target.getApplicationContext()'
@AnnotationConfigEmbeddedWebApplicationContext[
    reader=@AnnotatedBeanDefinitionReader[org.springframework.context.annotation.AnnotatedBeanDefinitionReader@2e457641],
    scanner=@ClassPathBeanDefinitionScanner[org.springframework.context.annotation.ClassPathBeanDefinitionScanner@6eb38026],
    annotatedClasses=null,
    basePackages=null,
]
Affect(row-cnt:1) cost in 439 ms.
```

## 获取spring bean，并调用函数

`tt -i 1000 -w 'target.getApplicationContext().getBean("helloWorldService").getHelloMessage()'`{{execute T2}}

结果是：

```bash
$ tt -i 1000 -w 'target.getApplicationContext().getBean("helloWorldService").getHelloMessage()'
@String[Hello World]
Affect(row-cnt:1) cost in 52 ms.
```
