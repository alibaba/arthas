
Similar to executing batch commands, the one-time commands are executed
synchronously. No need to create a session, no need to set the
`sessionId` option.

```json
{
  "action": "exec",
  "command": "<Arthas command line>"
}
```

For example, get the Arthas version number:

`curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"exec",
  "command":"version"
}
' | json_pp`{{execute T3}}

The response is as follows:

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
Response data format description:

* `state`: Request processing status, refer to the description of
  "Response Status".
*  `sessionId `: Arthas session ID, one-time command to automatically
   create and destroy temporary sessions.
*  `body.jobId`: The job ID of the command, all output results of the
   same job are the same jobId.
*  `body.jobStatus`: The job status of the command.
*  `body.timeExpired`: Whether the job execution timed out.
* `body/results`: Command execution results.

**Command result format description**

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

* `type` : The command result type, except for the special ones such as
  `status`, the others remain the same as the Arthas command name.
  Please refer to the section
  "Special command results".
*  `jobId` : The job ID of the command.
*  Other fields are the data of each different command.

Note: You can also use a one-time command to execute continuous output
commands such as watch/trace, but you can't interrupt the command
execution, and there may be hang up for a long time. Please refer to the
example in the
"Make watch command output a map object"
section.

Please try to deal with it in the following way:

* Set a reasonable `execTimeout` to forcibly interrupt the command
  execution after the timeout period is reached to avoid a long hang.
* Use the `-n` parameter to specify a smaller number of executions.
* Ensure the methods of the command matched can be successfully hit and
  the `condition-express` is written correctly. If the `watch/trace` does
  not hit, even if `-n 1` is specified, it will hang and wait until the
  execution timeout.
