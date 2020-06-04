
The following describes the commands for viewing `JVM` information in Arthas.

### sysprop

`sysprop`{{execute T2}} can print all System Properties information.

Specify a single key: `sysprop java.version`{{execute T2}}

It can also be filtered by `grep`: `sysprop | grep user`{{execute T2}}

Set a new value: `sysprop testKey testValue`{{execute T2}}

### sysenv

The `sysenv`{{execute T2}} command gets the environment variable. Similar to the `sysprop` command.


### jvm

The `jvm`{{execute T2}} command prints out various details of the `JVM`.


### dashboard


The `dashboard`{{execute T2}} command can view the real-time data panel of the current system.

Enter `Q`{{execute T2}} or `Ctrl+C` to exit the dashboard command.