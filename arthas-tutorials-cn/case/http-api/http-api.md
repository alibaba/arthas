
### 概览

Http API
提供类似RESTful的交互接口，请求和响应均为JSON格式的数据。相对于Telnet/WebConsole的输出非结构化文本数据，Http
API可以提供结构化的数据，支持更复杂的交互功能，比如特定应用场景的一系列诊断操作。


#### 访问地址

Http API接口地址为：`http://ip:port/api`，必须使用POST方式提交请求参数。如POST
`http://127.0.0.1:8563/api` 。

注意：telnet服务的3658端口与Chrome浏览器有兼容性问题，建议使用http端口8563来访问http接口。

#### 请求数据格式

```json
{
  "action": "exec",
  "requestId": "req112",
  "sessionId": "94766d3c-8b39-42d3-8596-98aee3ccbefb",
  "consumerId": "955dbd1325334a84972b0f3ac19de4f7_2",
  "command": "version",
  "execTimeout": "10000"
}
```

请求数据格式说明：

* `action` : 请求的动作/行为，可选值请参考"请求Action"小节。
*  `requestId` : 可选请求ID，由客户端生成。
*  `sessionId` : Arthas会话ID，一次性命令不需要设置会话ID。
*  `consumerId` : Arthas消费者ID，用于多人共享会话。
*  `command` : Arthas command line 。
*  `execTimeout` : 命令同步执行的超时时间(ms)，默认为30000。

注意: 不同的action使用到参数不同，根据具体的action来设置参数。

#### 请求Action

目前支持的请求Action如下： 

* `exec` : 同步执行命令，命令正常结束或者超时后中断命令执行后返回命令的执行结果。
*  `async_exec` : 异步执行命令，立即返回命令的调度结果，命令执行结果通过`pull_results`获取。
*  `interrupt_job` : 中断会话当前的命令，类似Telnet `Ctrl + c`的功能。
*  `pull_results` : 获取异步执行的命令的结果，以http 长轮询（long-polling）方式重复执行
*  `init_session` : 创建会话
*  `join_session` : 加入会话，用于支持多人共享同一个Arthas会话
*  `close_session` : 关闭会话

#### 响应状态

响应中的state属性表示请求处理状态，取值如下：

* `SCHEDULED`：异步执行命令时表示已经创建job并已提交到命令执行队列，命令可能还没开始执行或者执行中；
* `SUCCEEDED`：请求处理成功（完成状态）；
* `FAILED`：请求处理失败（完成状态），通常附带message说明原因；
* `REFUSED`：请求被拒绝（完成状态），通常附带message说明原因；
