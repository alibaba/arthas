

## reset

When Arthas executes commands such as watch/trace, it actually modifies the application's bytecode and inserts the enhanced code. These enhancement codes can be removed by explicitly executing the `reset`{{execute T2}} command.

## Exit/Stop Arthas

Arthas can be exited with the `exit`{{execute interrupt}} or `quit`{{execute interrupt}} command.

After exiting Arthas, you can also connect with `java -jar arthas-boot.jar`{{execute interrupt}} again.

## Stop Arthas

The `exit/quit` command simply exits the current session and the arthas server still runs in the target process.

To completely exit Arthas, you can execute the `stop`{{execute interrupt}} command.

