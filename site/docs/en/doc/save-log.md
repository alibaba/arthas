# Log command outputs

[`Log command outputs` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=save-log)

::: tip
Log command outputs for later analysis
:::

- By default, this behavior is turned off. To enable it, execute the command below:

  ```bash
  $ options save-result true
   NAME         BEFORE-VALUE  AFTER-VALUE
  ----------------------------------------
  save-result  false         true
  Affect(row-cnt:1) cost in 3 ms.
  ```

  If the message above is output on the console, then this behavior is enabled successfully.

- Log file path

  The command execution result will be save in `{user.home}/logs/arthas-cache/result.log`. Pls. clean it up regularly to save disk space.

## Use asynchronous job to log

Reference: [async](async.md)
