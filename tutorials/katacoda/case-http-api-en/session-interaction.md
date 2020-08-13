
Users create and manage Arthas sessions, which are suitable for complex
interactive processes. The access process is as follows:

* Create a session
* Join the session (optional)
* Pull command results
* Execute a series of commands
* Interrupt command execution
* Close the session

#### Create session

`curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"init_session"
}
'`{{execute T3}}

Response result:

```json
{
   "sessionId" : "b09f1353-202c-407b-af24-701b744f971e",
   "consumerId" : "5ae4e5fbab8b4e529ac404f260d4e2d1_1",
   "state" : "SUCCEEDED"
}
```
The new session ID is: `b09f1353-202c-407b-af24-701b744f971e`, and
consumer ID is: `5ae4e5fbab8b4e529ac404f260d4e2d1_1`.

Please take a note of your session ID, and replace `<sessionId>` below to manually execute related commands.

#### Join session

Specify the session ID to join, and the server will assign a new
consumer ID. Multiple consumers can receive the same command results of
target session. This interface is used to support multiple people
sharing the same session or refreshing the page to retrieve the session
history.

```bash
curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"join_session",
  "sessionId" : "<sessionId>"
}
'
```

Response result:

```json
{
   "consumerId" : "8f7f6ad7bc2d4cb5aa57a530927a95cc_2",
   "sessionId" : "b09f1353-202c-407b-af24-701b744f971e",
   "state" : "SUCCEEDED"
}
```

The new consumer ID is `8f7f6ad7bc2d4cb5aa57a530927a95cc_2 ` .

Please take a note of your consumer ID, and replace `<consumerId>` below to manually execute related commands.

#### Pull command results

The action of pulling the command result message is `pull_results`.
Please use the Http long-polling method to periodically pull the result
messages. The consumer's timeout period is 5 minutes. After the timeout,
you need to call `join_session` to allocate a new consumer.

Each consumer is allocated a cache queue separately, and the pull order
does not affect the content received by the consumer.


The request parameters require session ID and consumer ID:

```bash
curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"pull_results",
  "sessionId" : "<sessionId>",
  "consumerId" : "<consumerId>"
}
'
```

Use Bash scripts to regularly pull results messages:

```bash
while true; do curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"pull_results",
  "sessionId" : "<sessionId>",
  "consumerId" : "<consumerId>"
}
' | json_pp; sleep 2; done
```

Note: The `json_pp` tool formats the output content as pretty json.

The response content is as follows:

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


#### Execute commands asynchronously

```bash
curl -Ss -XPOST http://localhost:8563/api -d '''
{
  "action":"async_exec",
  "command":"watch demo.MathGame primeFactors \"{params, returnObj, throwExp}\" ",
  "sessionId" : "<sessionId>"
}
'''
```

Response of `async_exec`:

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

* `state` : The status of `SCHEDULED` means that the command has been
  parsed and generated the job, but the execution has not started.
* `body.jobId` : The job id of command execution, filter the command
  results output in `pull_results` according to this job ID.
* `body.jobStatus` : The job status `READY` means that execution has not started.

The shell output of the script that continuously pulls the result message:

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


The `value` of the watch command result is the value of watch-experss,
and the above command is `{params, returnObj, throwExp}`, so the value
of the watch result is an array of length 3, and each element
corresponds to the expression in the corresponding order.

Please refer to the section "[Make watch command output a map object](#change_watch_value_to_map)".

#### Interrupt command execution

Interrupt the running foreground job of the session:

```bash
curl -Ss -XPOST http://localhost:8563/api -d '''
{
  "action":"interrupt_job",
  "sessionId" : "<sessionId>"
}
'''
```

```json
{
   "state" : "SUCCEEDED",
   "body" : {
      "jobStatus" : "TERMINATED",
      "jobId" : 3
   }
}
```

#### Close session

Specify the session ID to close the session.

```bash
curl -Ss -XPOST http://localhost:8563/api -d '''
{
  "action":"close_session",
  "sessionId" : "<sessionId>"
}
'''
```

```json
{
   "state" : "SUCCEEDED"
}
```