> Global options

| Name                   | Default Value | Description                                                                                                                                                                                           |
| ---------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ---- | -------------------------------------------------------------------- |
| unsafe                 | false         | whether to enhance to system-level class. Use it with caution since JVM may hang                                                                                                                      |
| dump                   | false         | whether to dump enhanced class to the external files. If it's on, enhanced class will be dumped into `/${application dir}/arthas-class-dump/`, the specific output path will be output in the console |
| batch-re-transform     | true          | whether to re-transform matched classes in batch                                                                                                                                                      |
| json-format            | false         | whether to output in JSON format                                                                                                                                                                      |
| disable-sub-class      | false         | whether to enable matching child classes. The default value is `true`. If exact match is desire, turn off this flag                                                                                   |
| support-default-method | true          | whether to enable matching default method in interface. The default value is `true`. Refer to [#1105](https://github.com/alibaba/arthas/issues/1105)                                                  |
| save-result            | false         | whether to save execution result. All execution results will be saved to `~/logs/arthas-cache/result.log` when it's turned on                                                                         |
| job-timeout            | 1d            | default timeout for background jobs. Background job will be terminated once it's timed out (i.e. 1d, 2h, 3m, 25s)                                                                                     | print-parent-fields | true | This option enables print files in parent class, default value true. |

[options command Docs](https://arthas.aliyun.com/en/doc/options.html)

### View all options

`options`{{execute T2}}

### Get special option value

`options json-format`{{execute T2}}

> By default, `json-format` is false. When set `json-format` to true, commands like `wathc`/`tt` will print result with `json` format.

### Set special option value

For example, to enable saving command execution result, first check the log, and you can see there's no result:

`cat /root/logs/arthas-cache/result.log`{{execute T2}}

To enable saving command execution result, input the command below:

`options save-result true`{{execute T2}}

Wait for a second, and you will see there exist some results:

`cat /root/logs/arthas-cache/result.log`{{execute T2}}
