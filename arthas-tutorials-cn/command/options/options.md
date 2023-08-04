> 全局开关

| 名称                   | 默认值 | 描述                                                                                                                                                       |
| ---------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| unsafe                 | false  | 是否支持对系统级别的类进行增强，打开该开关可能导致把 JVM 搞挂，请慎重选择！                                                                                |
| dump                   | false  | 是否支持被增强了的类 dump 到外部文件中，如果打开开关，class 文件会被 dump 到`/${application working dir}/arthas-class-dump/`目录下，具体位置详见控制台输出 |
| batch-re-transform     | true   | 是否支持批量对匹配到的类执行 retransform 操作                                                                                                              |
| json-format            | false  | 是否支持 json 化的输出                                                                                                                                     |
| disable-sub-class      | false  | 是否禁用子类匹配，默认在匹配目标类的时候会默认匹配到其子类，如果想精确匹配，可以关闭此开关                                                                 |
| support-default-method | true   | 是否支持匹配到 default method，默认会查找 interface，匹配里面的 default method。参考 [#1105](https://github.com/alibaba/arthas/issues/1105)                |
| save-result            | false  | 是否打开执行结果存日志功能，打开之后所有命令的运行结果都将保存到`~/logs/arthas-cache/result.log`中                                                         |
| job-timeout            | 1d     | 异步后台任务的默认超时时间，超过这个时间，任务自动停止；比如设置 1d, 2h, 3m, 25s，分别代表天、小时、分、秒                                                 |
| print-parent-fields    | true   | 是否打印在 parent class 里的 filed                                                                                                                         |

[options 命令文档](https://arthas.aliyun.com/doc/options.html)

### 查看所有的 options

`options`{{execute T2}}

### 获取 option 的值

`options json-format`{{execute T2}}

> 默认情况下`json-format`为 false，如果希望`watch`/`tt`等命令结果以 json 格式输出，则可以设置`json-format`为 true。

### 设置指定的 option

例如，想打开执行结果存日志功能首先查看日志，发现无记录：

`cat /root/logs/arthas-cache/result.log`{{execute T2}}

输入如下命令即可激活记录日志功能：

`options save-result true`{{execute T2}}

稍候片刻，再次查看，发现出现记录：

`cat /root/logs/arthas-cache/result.log`{{execute T2}}
