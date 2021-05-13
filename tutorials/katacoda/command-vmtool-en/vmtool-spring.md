Next, use the `vmtool` command to find objects in spring.

### Find spring context

```bash
$ vmtool --action getInstances --className org.springframework.context.ApplicationContext
@ApplicationContext[][
    @AnnotationConfigEmbeddedWebApplicationContext[org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@12028586: startup date [Thu May 13 16:08:38 UTC 2021]; root of context hierarchy],
]
```

### Specify the number of expanded layers of returned results

> The return result of the `getInstances` action is bound to the `instances` variable, which is an array.

> The expansion level of the result can be specified by the `-x`/`--expand` parameter, the default value is 1.

```bash
vmtool --action getInstances -c 19469ea2 --className org.springframework.context.ApplicationContext -x 2
```

### Execute expression

> The return result of the `getInstances` action is bound to the `instances` variable, which is an array. The specified expression can be executed through the `--express` parameter.

Find the names of all spring beans:

```bash
vmtool --action getInstances --className org.springframework.context.ApplicationContext --express'instances[0].getBeanDefinitionNames()'
```

Call the `userController.findUserById(1)` function:

```
$ vmtool --action getInstances --className org.springframework.context.ApplicationContext --express'instances[0].getBean("userController").findUserById(1)'
@User[
    id=@Integer[1],
    name=@String[name1],
]
```

### Find all spring mapping objects

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
