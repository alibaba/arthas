

In this case, the user can get the spring context, get the bean, and invoke the method.

### Use the tt command to record the invocation of the specified method

`tt` is TimeTunnel, which records the parameters and return value of each invocation of the specified method.

* https://arthas.aliyun.com/doc/tt.html

`tt -t org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter invokeHandlerMethod`{{execute T2}}

Visit: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/1

You can see that the `tt` command record an invocation:

```bash
$ tt -t org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdaptePress Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 252 ms.
 INDE  TIMESTAMP    COST(  IS-R  IS-  OBJECT     CLASS               METHOD
 X                  ms)    ET    EXP
-----------------------------------------------------------------------------------------
 1000  2019-02-15   4.583  true  fal  0xc93cf1a  RequestMappingHand  invokeHandlerMethod
       15:38:32     923          se              lerAdapter
```

### Use the tt command to get the spring context from the invocation record.

Type `Q`{{execute T2}} or `Ctrl + C` to exit the `tt -t` command above.

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

## Get the spring bean and invoke method

`tt -i 1000 -w 'target.getApplicationContext().getBean("helloWorldService").getHelloMessage()'`{{execute T2}}

The result is:

```bash
$ tt -i 1000 -w 'target.getApplicationContext().getBean("helloWorldService").getHelloMessage()'
@String[Hello World]
Affect(row-cnt:1) cost in 52 ms.
```
