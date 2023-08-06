There is a very fundamental class `Advice` for the expressions used in filtering, tracing or monitoring and other aspects in commands.

## Fundamental Fields Table

|      Name | Specification                                                                                                                                                       |
| --------: | :------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
|    loader | the class loader for the current called class                                                                                                                       |
|     clazz | the reference to the current called class                                                                                                                           |
|    method | the reference to the current called method                                                                                                                          |
|    target | the instance of the current called class                                                                                                                            |
|    params | the parameters for the current call, which is an array (when there's no parameter, it will be an empty array)                                                       |
| returnObj | the return value from the current call - only available when the method call returns normally (`isReturn==true`), and `null` is for `void` return value             |
|  throwExp | the exceptions thrown from the current call - only available when the method call throws exception (`isThrow==true`)                                                |
|  isBefore | flag to indicate the method is about to execute. `isBefore==true` but `isThrow==false` and `isReturn==false` since it's no way to know how the method call will end |
|   isThrow | flag to indicate the method call ends with exception thrown                                                                                                         |
|  isReturn | flag to indicate the method call ends normally without exception thrown                                                                                             |

`watch com.example.demo.arthas.user.UserController  * "{loader, clazz, method, target, params, returnObj, throwExp, isBefore, isThrow, isReturn}"`{{exec}}

After running the above command, access [/user/1]({{TRAFFIC_HOST1_80}}/user/1) you can see the output of the corresponding variables.

The user can exit the watch command by typing `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}}.
