
> Load the external `*.class` files to retransform the loaded classes in JVM.

Reference: [Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)


### Options

|Name|Specification|
|---:|:---|
|`[c:]`|hashcode of the class loader|
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |
|`[p:]`|absolute path of the external `*.class`, multiple paths are separated with 'space'|

### Restrictions of the retransform command

* New field/method is not allowed
* The function that is running, no exit can not take effect.