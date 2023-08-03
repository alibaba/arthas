> 加载外部的`.class`文件，redefine jvm 已加载的类。

[redefine 命令文档](https://arthas.aliyun.com/doc/redefine.html)

参考：[Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

### 常见问题

- redefine 的 class 不能修改、添加、删除类的 field 和 method，包括方法参数、方法名称及返回值

- 如果 mc 失败，可以在本地开发环境编译好 class 文件，上传到目标系统，使用 redefine 热加载 class

- 目前 redefine 和 watch/trace/jad/tt 等命令冲突，以后重新实现 redefine 功能会解决此问题

> 注意，redefine 后的原来的类不能恢复，redefine 有可能失败（比如增加了新的 field），参考 jdk 本身的文档。

> `reset`命令对`redefine`的类无效。如果想重置，需要`redefine`原始的字节码。

> `redefine`命令和`jad`/`watch`/`trace`/`monitor`/`tt`等命令会冲突。执行完`redefine`之后，如果再执行上面提到的命令，则会把`redefine`的字节码重置。
> 原因是 jdk 本身 redefine 和 Retransform 是不同的机制，同时使用两种机制来更新字节码，只有最后修改的会生效。

### 参数说明

|              参数名称 | 参数说明                                   |
| --------------------: | :----------------------------------------- |
|                  [c:] | ClassLoader 的 hashcode                    |
| `[classLoaderClass:]` | 指定执行表达式的 ClassLoader 的 class name |
|                  [p:] | 外部的`.class`文件的完整路径，支持多个     |

### redefine 的限制

- 不允许新增加 field/method
- 正在跑的函数，没有退出不能生效。
