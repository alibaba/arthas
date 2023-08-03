在这个案例里，展示获取 spring context，再获取 bean，然后调用函数。

### 使用 tt 命令获取到 spring context

[tt](https://arthas.aliyun.com/doc/tt.html) 即 TimeTunnel，它可以记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测。

`tt -t org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter invokeHandlerMethod`{{execute T2}}

访问：[/user/1]({{TRAFFIC_HOST1_80}}/user/1)

可以看到`tt`命令捕获到了请求

### 使用 tt 命令从调用记录里获取到 spring context

输入 `Q`{{exec interrupt}} 或者 `Ctrl + C`{{exec interrupt}} 退出上面的 `tt -t`命令。  
使用 `tt -l`{{exec}} 命令可以查看之前 `tt` 命令捕获到的请求信息。

`tt -i 1000 -w 'target.getApplicationContext()'`{{execute T2}}

### 获取 spring bean，并调用函数

`tt -i 1000 -w 'target.getApplicationContext().getBean("helloWorldService").getHelloMessage()'`{{execute T2}}

### Vmtool 获取 HelloWorldService 对象实例，并调用函数

上面的方法使用 tt 命令获取 spring bean，并调用函数，有点繁琐，更换为使用 [vmtool](https://arthas.aliyun.com/doc/vmtool.html) 将会极大的简化这个流程，命令如下：

`vmtool --action getInstances --className com.example.demo.arthas.aop.HelloWorldService --express 'instances[0].getHelloMessage()'`{{exec}}
