redefine
========

> Load the external `*.class` files to re-define the loaded peer class in JVM.

Reference: [Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> Notes: Re-defined classes cannot be restored. There are chances that redefining may fail due to some reasons, for example: there's new field introduced in the new version of the class, pls. refer to JDK's documentation for the limitations.

### Options

|Name|Specification|
|---:|:---|
|`[c:]`|hashcode of the class loader|
|`[p:]`|absolute path of the external `*.class`, multiple paths are separated with 'space'|


### Usage

```
redefine -p /tmp/Test.class
redefine -c 327a647b -p /tmp/Test.class /tmp/Test$Inner.class
```
