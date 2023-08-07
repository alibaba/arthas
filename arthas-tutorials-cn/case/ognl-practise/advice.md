无论是匹配表达式也好、观察表达式也罢，他们核心判断变量都是围绕着一个 Arthas 中的通用通知对象 [Advice](https://arthas.aliyun.com/doc/advice-class.html) 进行。  
比如常见的（增加一下命令link） `watch`、`trace`、`tt`、`ognl`、`vmtool` 等命令都是居于 OGNL 表达式

## 核心变量列表

|    变量名 | 变量解释                                                                                                                                                                             |
| --------: | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|    loader | 本次调用类所在的 ClassLoader                                                                                                                                                         |
|     clazz | 本次调用类的 Class 引用                                                                                                                                                              |
|    method | 本次调用方法反射引用                                                                                                                                                                 |
|    target | 本次调用类的实例                                                                                                                                                                     |
|    params | 本次调用参数列表，这是一个数组，如果方法是无参方法则为空数组                                                                                                                         |
| returnObj | 本次调用返回的对象。当且仅当 `isReturn==true` 成立时候有效，表明方法调用是以正常返回的方式结束。如果当前方法无返回值 `void`，则值为 null                                             |
|  throwExp | 本次调用抛出的异常。当且仅当 `isThrow==true` 成立时有效，表明方法调用是以抛出异常的方式结束。                                                                                        |
|  isBefore | 辅助判断标记，当前的通知节点有可能是在方法一开始就通知，此时 `isBefore==true` 成立，同时 `isThrow==false` 和 `isReturn==false`，因为在方法刚开始时，还无法确定方法调用将会如何结束。 |
|   isThrow | 辅助判断标记，当前的方法调用以抛异常的形式结束。                                                                                                                                     |
|  isReturn | 辅助判断标记，当前的方法调用以正常返回的形式结束。                                                                                                                                   |

`watch com.example.demo.arthas.user.UserController  * "{loader, clazz, method, target, params, returnObj, throwExp, isBefore, isThrow, isReturn}"`{{exec}}

运行上面上面命令后访问 [/user/1]({{TRAFFIC_HOST1_80}}/user/1) 你可以查看到对应变量的输出。

输入 `Q`{{exec interrupt}} 或者 `Ctrl+C`{{exec interrupt}} 退出 watch 命令。
