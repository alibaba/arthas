### 当 ID 大于 10 的用户访问时输出用户 ID

`watch com.example.demo.arthas.user.UserController  * "{params[0]}" "params[0] > 10"`{{exec}}

运行上面命令后访问 [/user/12]({{TRAFFIC_HOST1_80}}/user/12) 可以看到终端上打印出了访问用户的 ID  
如果访问的是 [/user/9]({{TRAFFIC_HOST1_80}}/user/9) 则不会有输出。

输入 `Q`{{exec interrupt}} 或者 `Ctrl+C`{{exec interrupt}} 退出 watch 命令。

### 调用类中入参的对象的方法

对 `com.example.demo.arthas.WelcomeController` 类当中的 `helloWorld` 的 `model` 入参调用 `addAttribute` 方法修改 `name` 的值  
先访问 [/hello]({{TRAFFIC_HOST1_80}}/hello) 可以看到页面显示的是 `Hello World - jsp`。

`watch com.example.demo.arthas.WelcomeController helloWorld "{params[0].addAttribute('name', 'HTML')}"`{{exec}}

运行上面命令后，再次访问 [/hello]({{TRAFFIC_HOST1_80}}/hello) 可以看到页面显示内容已变为 `Hello World - HTML`。

输入 `Q`{{exec interrupt}} 或者 `Ctrl+C`{{exec interrupt}} 退出 watch 命令。

### 当访问出现错误时输出入参，调用类的实例，返回值以及抛出的错误

`watch com.example.demo.arthas.user.UserController  * "{params, target, returnObj, throwExp}" "throwExp != null"`{{exec}}

运行上面命令后访问 [/user/0]({{TRAFFIC_HOST1_80}}/user/0) 可以看到输出了入参，调用类的实例，返回值以及抛出的错误。

输入 `Q`{{exec interrupt}} 或者 `Ctrl+C`{{exec interrupt}} 退出 watch 命令。

### 内部类案例

先通过 ` sc  '*＄*'  | grep java.lang.Integer`{{exec}} 查找内部类

之后使用 `ognl -x 3 '@java.lang.Integer$IntegerCache@low'`{{exec}} 查看

## 补充

还有更多有用的有趣的例子请查看相关 Issues[#71](https://github.com/alibaba/arthas/issues/71)。
