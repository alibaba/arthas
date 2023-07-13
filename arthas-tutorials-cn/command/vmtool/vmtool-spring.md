下面使用`vmtool`命令查找spring里的对象。

### 查找spring context

`vmtool --action getInstances --className org.springframework.context.ApplicationContext `{{execute T2}}

```bash
$ vmtool --action getInstances --className org.springframework.context.ApplicationContext 
@ApplicationContext[][
    @AnnotationConfigEmbeddedWebApplicationContext[org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@12028586: startup date [Thu May 13 16:08:38 UTC 2021]; root of context hierarchy],
]
```

### 指定返回结果展开层数

> `getInstances` action返回结果绑定到`instances`变量上，它是数组。

> 通过 `-x`/`--expand` 参数可以指定结果的展开层次，默认值是1。

`vmtool --action getInstances --className org.springframework.context.ApplicationContext -x 2`{{execute T2}}

### 执行表达式

> `getInstances` action返回结果绑定到`instances`变量上，它是数组。可以通过`--express`参数执行指定的表达式。

查找所有的spring beans名字：


`vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBeanDefinitionNames()'`{{execute T2}}


调用`userController.findUserById(1)`函数：


`vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBean("userController").findUserById(1)'`{{execute T2}}

```
$ vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBean("userController").findUserById(1)'
@User[
    id=@Integer[1],
    name=@String[name1],
]
```

### 查找所有的spring mapping对象


`vmtool --action getInstances --className org.springframework.web.servlet.HandlerMapping`{{execute T2}}

```bash
$ vmtool --action getInstances --className org.springframework.web.servlet.HandlerMapping
@HandlerMapping[][
    @SimpleUrlHandlerMapping[org.springframework.web.servlet.handler.SimpleUrlHandlerMapping@5d3819c8],
    @EmptyHandlerMapping[org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport$EmptyHandlerMapping@11d509ba],
    @RequestMappingHandlerMapping[org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping@56a5f2e3],
    @WelcomePageHandlerMapping[org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration$WelcomePageHandlerMapping@4c0a4ed3],
    @EmptyHandlerMapping[org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport$EmptyHandlerMapping@51e1f8c3],
    @BeanNameUrlHandlerMapping[org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping@68c0a39c],
    @SimpleUrlHandlerMapping[org.springframework.web.servlet.handler.SimpleUrlHandlerMapping@110b768d],
]
```
