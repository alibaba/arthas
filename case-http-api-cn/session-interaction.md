
由用户创建及管理Arthas会话，适用于复杂的交互过程。访问流程如下：

* 创建会话
* 加入会话(可选）
* 拉取命令结果
* 执行一系列命令
* 中断命令执行
* 关闭会话

#### 创建会话

创建会话, 保存输出到bash环境变量

`session_data=$(curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"init_session"
}
')
echo $session_data | json_pp`{{execute T3}}

注： `json_pp` 工具将输出内容格式化为pretty json。

响应结果：

```json
{
   "sessionId" : "b09f1353-202c-407b-af24-701b744f971e",
   "consumerId" : "5ae4e5fbab8b4e529ac404f260d4e2d1_1",
   "state" : "SUCCEEDED"
}
```

提取会话ID和消费者ID。

当前会话ID为： 

`session_id=$(echo $session_data | sed 's/.*"sessionId":"\([^"]*\)".*/\1/g')
echo $session_id`{{execute T3}}

`b09f1353-202c-407b-af24-701b744f971e`;

请记下这里的会话ID，在Terminal 4中需要手动输入。

当前消费者ID为：

`consumer_id=$(echo $session_data | sed 's/.*"consumerId":"\([^"]*\)".*/\1/g')
echo $consumer_id`{{execute T3}}

`5ae4e5fbab8b4e529ac404f260d4e2d1_1 `。

#### 加入会话

指定要加入的会话ID，服务端将分配一个新的消费者ID。多个消费者可以接收到同一个会话的命令结果。本接口用于支持多人共享同一个会话或刷新页面后重新拉取会话历史记录。

加入会话，保存输出到bash环境变量

`session_data=$(curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"join_session",
  "sessionId" : "'"$session_id"'"
}
')
echo $session_data | json_pp`{{execute T3}}

响应结果：

```json
{
   "consumerId" : "8f7f6ad7bc2d4cb5aa57a530927a95cc_2",
   "sessionId" : "b09f1353-202c-407b-af24-701b744f971e",
   "state" : "SUCCEEDED"
}
```

提取消费者ID。

新的消费者ID为

`consumer_id=$(echo $session_data | sed 's/.*"consumerId":"\([^"]*\)".*/\1/g')
echo $consumer_id`{{execute T3}}

`8f7f6ad7bc2d4cb5aa57a530927a95cc_2 ` 。

请记下这里的消费者ID，在Terminal 4中需要手动输入。

#### 拉取命令结果

拉取命令结果消息的action为`pull_results`。请使用Http long-polling方式，定时循环拉取结果消息。
消费者的超时时间为5分钟，超时后需要调用`join_session`分配新的消费者。每个消费者单独分配一个缓存队列，按顺序拉取命令结果，不会影响到其它消费者。

请求参数需要指定会话ID及消费者ID:

`curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"pull_results",
  "sessionId" : "'"$session_id"'",
  "consumerId" : "'"$consumer_id"'"
}
' | json_pp`{{execute T3}}

用Bash脚本定时拉取结果消息:

请在Terminal 4中输入Terminal 3中的会话ID，这里的例子如下：

`b09f1353-202c-407b-af24-701b744f971e`

`echo -n "Enter your sessionId in T3:"
read  session_id`{{execute T4}}

同样，接着输入Terminal 3中的消费者ID，这里的例子如下：

`8f7f6ad7bc2d4cb5aa57a530927a95cc_2 `

`echo -n "Enter your consumerId in T3:"
read  consumer_id`{{execute T4}}

`while true; do curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"pull_results",
  "sessionId" : "'"$session_id"'",
  "consumerId" : "'"$consumer_id"'"
}
' | json_pp; sleep 2; done`{{execute T4}}

响应内容如下：

```json
{
   "body" : {
      "results" : [
         {
            "inputStatus" : "DISABLED",
            "jobId" : 0,
            "type" : "input_status"
         },
         {
            "type" : "message",
            "jobId" : 0,
            "message" : "Welcome to arthas!"
         },
         {
            "tutorials" : "https://arthas.aliyun.com/doc/arthas-tutorials.html",
            "time" : "2020-08-06 15:56:43",
            "type" : "welcome",
            "jobId" : 0,
            "pid" : "7909",
            "wiki" : "https://arthas.aliyun.com/doc",
            "version" : "3.3.7"
         },
         {
            "inputStatus" : "ALLOW_INPUT",
            "type" : "input_status",
            "jobId" : 0
         }
      ]
   },
   "sessionId" : "b09f1353-202c-407b-af24-701b744f971e",
   "consumerId" : "8f7f6ad7bc2d4cb5aa57a530927a95cc_2",
   "state" : "SUCCEEDED"
}

```


#### 异步执行命令

`curl -Ss -XPOST http://localhost:8563/api -d '''
{
  "action":"async_exec",
  "command":"watch demo.MathGame primeFactors \"{params, returnObj, throwExp}\" ",
  "sessionId" : "'"$session_id"'"
}
''' | json_pp`{{execute T3}}

`async_exec` 的结果：

```json
{
   "sessionId" : "2b085b5d-883b-4914-ab35-b2c5c1d5aa2a",
   "state" : "SCHEDULED",
   "body" : {
      "jobStatus" : "READY",
      "jobId" : 3,
      "command" : "watch demo.MathGame primeFactors \"{params, returnObj, throwExp}\" "
   }
}
```

* `state` : `SCHEDULED` 状态表示已经解析命令生成任务，但未开始执行。
* `body.jobId` :
  异步执行命令的任务ID，可以根据此任务ID来过滤在`pull_results`输出的命令结果。
* `body.jobStatus` : 任务状态`READY`表示未开始执行。


切换到上面自动拉取结果消息脚本的shell（Terminal 4），查看输出：

```json
{
   "body" : {
      "results" : [
         {
            "type" : "command",
            "jobId" : 3,
            "state" : "SCHEDULED",
            "command" : "watch demo.MathGame primeFactors \"{params, returnObj, throwExp}\" "
         },
         {
            "inputStatus" : "ALLOW_INTERRUPT",
            "jobId" : 0,
            "type" : "input_status"
         },
         {
            "success" : true,
            "jobId" : 3,
            "effect" : {
               "listenerId" : 3,
               "cost" : 24,
               "classCount" : 1,
               "methodCount" : 1
            },
            "type" : "enhancer"
         },
         {
            "sizeLimit" : 10485760,
            "expand" : 1,
            "jobId" : 3,
            "type" : "watch",
            "cost" : 0.071499,
            "ts" : 1596703453237,
            "value" : [
               [
                  -170365
               ],
               null,
               {
                  "stackTrace" : [
                     {
                        "className" : "demo.MathGame",
                        "classLoaderName" : "app",
                        "methodName" : "primeFactors",
                        "nativeMethod" : false,
                        "lineNumber" : 46,
                        "fileName" : "MathGame.java"
                     },
                     ...
                  ],
                  "localizedMessage" : "number is: -170365, need >= 2",
                  "@type" : "java.lang.IllegalArgumentException",
                  "message" : "number is: -170365, need >= 2"
               }
            ]
         },
         {
            "type" : "watch",
            "cost" : 0.033375,
            "jobId" : 3,
            "ts" : 1596703454241,
            "value" : [
               [
                  1
               ],
               [
                  2,
                  2,
                  2,
                  2,
                  13,
                  491
               ],
               null
            ],
            "sizeLimit" : 10485760,
            "expand" : 1
         }
      ]
   },
   "consumerId" : "8ecb9cb7c7804d5d92e258b23d5245cc_1",
   "sessionId" : "2b085b5d-883b-4914-ab35-b2c5c1d5aa2a",
   "state" : "SUCCEEDED"
}
```

watch命令结果的`value`为watch-experss的值，上面命令中为`{params, returnObj,
throwExp}`，所以watch结果的value为一个长度为3的数组，每个元素分别对应相应顺序的表达式。
请参考"watch命令输出map对象"小节。

#### 中断命令执行

中断会话正在运行的前台Job（前台任务）：

`curl -Ss -XPOST http://localhost:8563/api -d '''
{
  "action":"interrupt_job",
  "sessionId" : "'"$session_id"'"
}
''' | json_pp`{{execute T3}}

```json
{
   "state" : "SUCCEEDED",
   "body" : {
      "jobStatus" : "TERMINATED",
      "jobId" : 3
   }
}
```

#### 关闭会话
指定会话ID，关闭会话。

`curl -Ss -XPOST http://localhost:8563/api -d '''
{
  "action":"close_session",
  "sessionId" : "'"$session_id"'"
}
''' | json_pp`{{execute T3}}

```json
{
   "state" : "SUCCEEDED"
}
```
