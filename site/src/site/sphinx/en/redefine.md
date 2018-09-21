redefine
===

> Load external `.class` files，redefine classes that jvm already loads。

Reference：[Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> Caution: classes that have been redefined can not be reverted，redefine may fail（add new field for instance），refer JVM original document.

### Parameters explanations

|Parameter Name| Parameter Description|
|---:|:---|
|[c:]|ClassLoader的hashcode|
|[p:]|full path of external `.class` files, support multiple files at a time|


### Usage

```
   redefine -p /tmp/Test.class
   redefine -c 327a647b -p /tmp/Test.class /tmp/Test\$Inner.class
```