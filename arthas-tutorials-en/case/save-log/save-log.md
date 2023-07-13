

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

* Log file path

The command execution result will be save in `{user.home}/logs/arthas-cache/result.log` Pls. clean it up regularly to save disk space.

## Use asynchronous job to log

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/admin/logs/arthas-cache/28198/2
```

* Reference: https://arthas.aliyun.com/doc/async.html
