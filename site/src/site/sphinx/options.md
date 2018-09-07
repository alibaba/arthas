options
===

> 全局开关

| 名称                | 默认值   | 描述                                       |
| ------------------ | ----- | ---------------------------------------- |
| unsafe             | false | 是否支持对系统级别的类进行增强，打开该开关可能导致把JVM搞挂，请慎重选择！   |
| dump               | false | 是否支持被增强了的类dump到外部文件中，如果打开开关，class文件会被dump到`/${application dir}/arthas-class-dump/`目录下，具体位置详见控制台输出 |
| batch-re-transform | true  | 是否支持批量对匹配到的类执行retransform操作              |
| json-format        | false | 是否支持json化的输出                             |
| disable-sub-class  | false | 是否禁用子类匹配，默认在匹配目标类的时候会默认匹配到其子类，如果想精确匹配，可以关闭此开关 |
| debug-for-asm      | false | 打印ASM相关的调试信息                             |
| save-result        | false | 是否打开执行结果存日志功能，打开之后所有命令的运行结果都将保存到`/home/admin/logs/arthas/arthas.log`中 |
| job-timeout        | 1d    | 异步后台任务的默认超时时间，超过这个时间，任务自动停止；比如设置 1d, 2h, 3m, 25s，分别代表天、小时、分、秒 |

### 使用说明

例如，想打开执行结果存日志功能，输入如下命令即可：

```
$ options save-result true                                                                                         
 NAME         BEFORE-VALUE  AFTER-VALUE                                                                            
----------------------------------------                                                                           
 save-result  false         true
```