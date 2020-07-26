
> Load the external `*.class` files to re-define the loaded classes in JVM.

Reference: [Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> Notes: Re-defined classes cannot be restored. There are chances that redefining may fail due to some reasons, for example: there's new field introduced in the new version of the class, pls. refer to JDK's documentation for the limitations.

> The `reset` command is not valid for classes that have been processed by `redefine`. If you want to reset, you need `redefine` the original bytecode.


> The `redefine` command will conflict with the `jad`/`watch`/`trace`/`monitor`/`tt` commands. After executing `redefine`, if you execute the above mentioned command, the bytecode of the class will be reset.
> The reason is that in the JDK `redefine` and `retransform` are different mechanisms. When two mechanisms are both used to update the bytecode, only the last modified will take effect.

### Options

|Name|Specification|
|---:|:---|
|`[c:]`|hashcode of the class loader|
|`[p:]`|absolute path of the external `*.class`, multiple paths are separated with 'space'|

### Restrictions of the redefine command

* New field/method is not allowed
* The function that is running, no exit can not take effect.