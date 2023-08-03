下面使用`vmtool`命令查找 spring 里的对象。

### 查找 spring context

`vmtool --action getInstances --className org.springframework.context.ApplicationContext `{{execute T2}}

### 指定返回结果展开层数

> `getInstances` action 返回结果绑定到`instances`变量上，它是数组。

> 通过 `-x`/`--expand` 参数可以指定结果的展开层次，默认值是 1。

`vmtool --action getInstances --className org.springframework.context.ApplicationContext -x 2`{{execute T2}}

### 执行表达式

> `getInstances` action 返回结果绑定到`instances`变量上，它是数组。可以通过`--express`参数执行指定的表达式。

查找所有的 spring beans 名字：

`vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBeanDefinitionNames()'`{{execute T2}}

调用`userController.findUserById(1)`函数：

`vmtool --action getInstances --className org.springframework.context.ApplicationContext --express 'instances[0].getBean("userController").findUserById(1)'`{{execute T2}}

### 查找所有的 spring mapping 对象

`vmtool --action getInstances --className org.springframework.web.servlet.HandlerMapping`{{execute T2}}
