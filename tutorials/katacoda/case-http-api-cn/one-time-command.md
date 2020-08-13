
与执行批处理命令类似，一次性命令以同步方式执行。不需要创建会话，不需要设置`sessionId`选项。

```json
{
  "action": "exec",
  "command": "<Arthas command line>"
}
```

比如获取Arthas版本号：

`curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"exec",
  "command":"version"
}
' | json_pp`{{execute T3}}

响应内容如下：

```json
{
   "state" : "SUCCEEDED",
   "sessionId" : "ee3bc004-4586-43de-bac0-b69d6db7a869",
   "body" : {
      "results" : [
         {
            "type" : "version",
            "version" : "3.3.7",
            "jobId" : 5
         },
         {
            "jobId" : 5,
            "statusCode" : 0,
            "type" : "status"
         }
      ],
      "timeExpired" : false,
      "command" : "version",
      "jobStatus" : "TERMINATED",
      "jobId" : 5
   }
}
```

响应数据解析：

* `state`: 请求处理状态，参考“接口响应状态”说明
*  `sessionId `: Arthas会话ID，一次性命令自动创建及销毁临时会话
*  `body.jobId`:  命令的任务ID，同一任务输出的所有Result都是相同的jobId
*  `body.jobStatus`: 任务状态，同步执行正常结束为`TERMINATED `
*  `body.timeExpired`: 任务执行是否超时
* `body/results`: 命令执行的结果列表

**命令结果格式说明**

```json
 [{
    "type" : "version",
    "version" : "3.3.7",
    "jobId" : 5
 },
 {
    "jobId" : 5,
    "statusCode" : 0,
    "type" : "status"
 }]
```

* `type` : 命令结果类型，除了`status`等特殊的几个外，其它的保持与Arthas命令名称一致。请参考"特殊命令结果"小节。
*  `jobId` : 处理命令的任务ID。
*  其它字段为每个不同命令的数据。

注意：也可以使用一次性命令的方式执行watch/trace等连续输出的命令，但不能中断命令执行，可能出现长时间没有结束的问题。请参考"watch命令输出map对象"小节的示例。

请尽量按照以下方式处理：

* 设置合理的`execTimeout`，到达超时时间后强制中断命令执行，避免长时间挂起。
* 通过`-n`参数指定较少的执行次数。
* 保证命令匹配的方法可以成功命中和condition-express编写正确，如果watch/trace没有命中就算指定`-n
  1`也会挂起等待到执行超时。
