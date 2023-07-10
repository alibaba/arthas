
`reset` can reset enhanced classes. All enhanced classes will be reset to their original states. When Arthas server closes, all these enhanced classes will be reset too.

When Arthas executes commands such as watch/trace, it actually modifies the application's bytecode and inserts the enhanced code. These enhancement codes can be removed by explicitly executing the `reset`{{execute T2}} command.