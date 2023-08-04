In this case, the user can get the spring context, get the bean, and invoke the method.

### Use the tt command to record the invocation of the specified method

[tt](https://arthas.aliyun.com/doc/tt.html) is TimeTunnel, which records the parameters and return value of each invocation of the specified method.

`tt -t org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter invokeHandlerMethod`{{execute T2}}

Visit: [/user/1]({{TRAFFIC_HOST1_80}}/user/1)

You can see the `tt` command record an invocation

### Use the tt command to get the spring context from the invocation record.

Type `Q`{{exec interrupt}} or `Ctrl + C`{{exec interrupt}} to exit the `tt -t` command above.

`tt -i 1000 -w 'target.getApplicationContext()'`{{execute T2}}

## Get the spring bean and invoke method

`tt -i 1000 -w 'target.getApplicationContext().getBean("helloWorldService").getHelloMessage()'`{{execute T2}}

## Use the vmtool command to get the spring bean and invoke method

The above process of using the tt command to retrieve Spring beans and call functions, can be a bit cumbersome. However, you can simplify this process significantly by using vmtool. The command for this would be as follows:

`vmtool --action getInstances --className com.example.demo.arthas.aop.HelloWorldService --express 'instances[0].getHelloMessage()'`{{exec}}
