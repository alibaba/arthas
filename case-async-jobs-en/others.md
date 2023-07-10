
* Support up to 8 commands at the same time to redirect the output to the log files.

* Do not open too many background jobs at the same time to avoid negative performance effect to the target JVM.

* If you do not want to stop the Arthas service and continue to perform background tasks, you can exit the Arthas console by executing `quit` command (`stop` command will stop the Arthas service)
