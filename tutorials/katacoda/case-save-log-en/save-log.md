

Log command outputs for later analysis

By default, this behavior is turned off. To enable it, execute the command below:

`options save-result true`{{execute T2}}

```bash
$ options save-result true
 NAME         BEFORE-VALUE  AFTER-VALUE
----------------------------------------
save-result  false         true
Affect(row-cnt:1) cost in 3 ms.
```

If the message above is output on the console, then this behavior is enabled successfully.

## Log file path

The command execution result will be save in `{user.home}/logs/arthas-cache/result.log` Pls. clean it up regularly to save disk space.

Use asynchronous job to log
$ trace Test t >>  &
job id  : 2
cache location  : /Users/zhuyong/logs/arthas-cache/28198/2
By doing this, the command will run at background asynchronously, and output the execution result into `~/logs/arthas-cache/{PID}/{JobId}`:

The background job will continue to run even if the current session is disconnected. The default job timeout value is 1 day, use global ‘[options](https://arthas.aliyun.com/doc/en/options.html)’ command to alternate it.

The execution result will be output into the file asynchronously. Pls. note the behavior of this command is not controlled by save-result option from global [options](https://arthas.aliyun.com/doc/en/options.html). No matter save-result is set to true or not, this command will write into `~/logs/arthas-cache/{PID}/{JobId}` anyway, instead of into `~/logs/arthas-cache/result.log`.
