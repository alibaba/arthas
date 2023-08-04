Log command outputs for later analysis

By default, this behavior is turned off. To enable it, execute the command below:

`options save-result true`{{execute T2}}

If the message above is output on the console, then this behavior is enabled successfully.

- Log file path

The command execution result will be save in `{user.home}/logs/arthas-cache/result.log` Pls. clean it up regularly to save disk space.

## Use asynchronous job to log

Running `trace demo.MathGame run >> a.log &`{{exec}} will suspend the task in the background. You can use `jobs`{{exec}} to view the currently running background tasks and then use `kill` with job id to end them.

You can use `cat a.log`{{exec}} to see that the output of the running command has been redirected to the `a.log` file.

- Reference: https://arthas.aliyun.com/doc/async.html
