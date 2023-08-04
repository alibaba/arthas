# monitor

> Monitor method invocation.

Monitor invocation for the method matched with `class-pattern` and `method-pattern`.

[monitor command](https://arthas.aliyun.com/en/doc/monitor.html) is not a command returning immediately.

A command returning immediately is a command immediately returns with the result after the command is input, while a non-immediate returning command will keep outputting the information from the target JVM process until user presses `Ctrl+C`.

On Arthas's server side, the command is running as a background job, but the weaved code will not take further effect once the job is terminated, therefore, it will not impact the performance after the job quits. Furthermore, Arthas is designed to have no side effect to the business logic.

### Items to monitor

|      Item | Specification                            |
| --------: | :--------------------------------------- |
| timestamp | timestamp                                |
|     class | Java class                               |
|    method | method (constructor and regular methods) |
|     total | calling times                            |
|   success | success count                            |
|      fail | failure count                            |
|        rt | average RT                               |
| fail-rate | failure ratio                            |

### Usage

`monitor -c 5 demo.MathGame primeFactors`{{execute T2}}

Press `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}} to abort
