redefine
========

Load the external `*.class` files and *re-define* the JVM-loaded classes.

Reference: [Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

F.Y.I

1. Re-defined classes cannot be restores any more;
2. Re-definition can fail (like adding a new field); for more information, please refer to JDK documentation

### Options

|Name|Specification|
|---:|:---|
|[c:]|hashcode of the class loader|
|[p:]|absolute path of the external `*.class` (multiple paths supported)|


### Usage

```
   redefine -p /tmp/Test.class
   redefine -c 327a647b -p /tmp/Test.class /tmp/Test\$Inner.class
```
