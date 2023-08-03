> 方法执行监控

对匹配 `class-pattern`／`method-pattern`的类、方法的调用进行监控。

[monitor 命令](https://arthas.aliyun.com/doc/monitor.html)是一个非实时返回命令。

实时返回命令是输入之后立即返回，而非实时返回的命令，则是不断的等待目标 Java 进程返回信息，直到用户输入 `Ctrl+C` 为止。

服务端是以任务的形式在后台跑任务，植入的代码随着任务的中止而不会被执行，所以任务关闭后，不会对原有性能产生太大影响，而且原则上，任何 Arthas 命令不会引起原有业务逻辑的改变。

### 监控的维度说明

|    监控项 | 说明                       |
| --------: | :------------------------- |
| timestamp | 时间戳                     |
|     class | Java 类                    |
|    method | 方法（构造方法、普通方法） |
|     total | 调用次数                   |
|   success | 成功次数                   |
|      fail | 失败次数                   |
|        rt | 平均 RT                    |
| fail-rate | 失败率                     |

### 使用参考

`monitor -c 5 demo.MathGame primeFactors`{{execute T2}}

按`Q`{{exec interrupt}}或者`Ctrl+c`{{exec interrupt}}退出
