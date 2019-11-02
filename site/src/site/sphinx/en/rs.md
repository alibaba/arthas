rs
========

> Compile *.java files into bytecode, which will be loaded by a standalone classloader.
> Then run the first main method by reflection.

Reference: [mc](mc.md)

### Options

|Name|Specification|
|---:|:---|
|`[c:]`|hashcode of the class loader|
|`[encoding:]`|encoding of the java files|


### Usage

```bash
rs -c 327a647b /tmp/TestMain.java
```