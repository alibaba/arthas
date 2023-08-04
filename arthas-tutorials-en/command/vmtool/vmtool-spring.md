Next, use the `vmtool` command to find objects in spring.

### Find spring context

`vmtool --action getInstances --className org.springframework.context.ApplicationContext `{{execute T2}}

### Specify the number of expanded layers of returned results

> The return result of the `getInstances` action is bound to the `instances` variable, which is an array.

> The expansion level of the result can be specified by the `-x`/`--expand` parameter, the default value is 1.

`vmtool --action getInstances --className org.springframework.context.ApplicationContext -x 2`{{execute T2}}

### Execute expression

> The return result of the `getInstances` action is bound to the `instances` variable, which is an array. The specified expression can be executed through the `--express` parameter.

Find the names of all spring beans:

`vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBeanDefinitionNames()'`{{execute T2}}

Call the `userController.findUserById(1)` method:

`vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBean("userController").findUserById(1)'`{{execute T2}}

### Find all spring mapping objects

`vmtool --action getInstances --className org.springframework.web.servlet.HandlerMapping`{{execute T2}}
