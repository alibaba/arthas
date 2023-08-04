The following describes the commands for viewing `JVM` information in Arthas.

### [sysprop](https://arthas.aliyun.com/en/doc/sysprop.html)

`sysprop`{{execute T2}} can print all System Properties information.

Specify a single key: `sysprop java.version`{{execute T2}}

It can also be filtered by `grep`: `sysprop | grep user`{{execute T2}}

Set a new value: `sysprop testKey testValue`{{execute T2}}

### [sysenv](https://arthas.aliyun.com/en/doc/sysenv.html)

The `sysenv`{{execute T2}} command gets the environment variable. Similar to the `sysprop` command.

### [jvm](https://arthas.aliyun.com/en/doc/jvm.html)

The `jvm`{{execute T2}} command prints out various details of the `JVM`.

### [dashboard](https://arthas.aliyun.com/en/doc/dashboard.html)

The `dashboard`{{execute T2}} command can view the real-time data panel of the current system.

Enter `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}} to exit the dashboard command.
