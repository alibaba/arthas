redefine
========

Load the external `*.class` files to **re-define** the JVM-loaded classes.

Reference: [Instrumentation#redefineClasses](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses(java.lang.instrument.ClassDefinition...))

**You should know:**
1. re-defined classes cannot be restored any more;
2. re-definition can fail (e.g. adding a new field/method); for more information, please refer to JDK documentation

### Options

|Name|Specification|
|---:|:---|
|[c:]|hashcode of the class loader|
|[p:]|absolute path of the external `*.class` (multiple paths supported)|


### Usage

```bash
# make sure the demo is running
javac demo/Demo.java && java demo/Demo
```

Update the method:

```java
# change the increment method

# from
public synchronized static void increment() {
    count.incrementAndGet();
}

# to
public synchronized static void increment() {
    count.decrementAndGet();
}
```

When the demo is still running, let's hot-load the updated method as:

```bash
javac demo/Demo.java

$ redefine -p demo/Demo$Counter.class
redefine success, size: 1
```

If you want to use specific loader: 

```bash
# locate which loader
$ sc -d demo.Demo
 class-info        demo.Demo                                                                                                                
 code-source       /Users/hello/test/                                                                                                     
 name              demo.Demo                                                                                                                
 isInterface       false                                                                                                                    
 isAnnotation      false                                                                                                                    
 isEnum            false                                                                                                                    
 isAnonymousClass  false                                                                                                                    
 isArray           false                                                                                                                    
 isLocalClass      false                                                                                                                    
 isMemberClass     false                                                                                                                    
 isPrimitive       false                                                                                                                    
 isSynthetic       false                                                                                                                    
 simple-name       Demo                                                                                                                     
 modifier          public                                                                                                                   
 annotation                                                                                                                                 
 interfaces                                                                                                                                 
 super-class       +-java.lang.Object                                                                                                       
 class-loader      +-sun.misc.Launcher$AppClassLoader@659e0bfd                                                                              
                     +-sun.misc.Launcher$ExtClassLoader@758c1b43                                                                            
 classLoaderHash   659e0bfd                                                                                                                 

Affect(row-cnt:1) cost in 14 ms.

# use the specific loader
$ redefine -c 659e0bfd -p demo/Demo$Counter.class
redefine success, size: 1
```

