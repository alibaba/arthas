sc
==

Check the profiles/details of the loaded classes (abbreviated from *Search Class*)

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|[d]|print the details of the current class including the source file, class declaration, the class loaders and more. (F.Y.I if a class is loaded by several class loaders, then the class will be printed several times)|
|[E]|turn on regex matching while the default is wildcard matching|
|[f]|print the fields info of the current class, which **must** be used with `-d`|
|[x:]|the depth to print the static fields, whose default is `0` - directly invoking `toString()`|

Tip: 
1. *class-patten* supports two full qualified class name formats as `your.full.class.name` and `your/full/class/class` (e.g. `com.taobao.test.AAA` and `com/taobao/test/AAA`);
2. `sc` turned on the `sub-classes` matching in default; if you want to hide `sub-classes`, you can turn it off with [options](options.md) as `options disable-sub-class true`.

### Usage

Check the static fields of a class using `sc -df class-name`

```shell
$ sc demo.Demo$Counter
demo.Demo$Counter
Affect(row-cnt:1) cost in 12 ms.
$ sc -df demo.Demo$Counter 
 class-info        demo.Demo$Counter                                                                                                                                                 
 code-source       /Users/hello/test/                                                                                                                                              
 name              demo.Demo$Counter                                                                                                                                                 
 isInterface       false                                                                                                                                                             
 isAnnotation      false                                                                                                                                                             
 isEnum            false                                                                                                                                                             
 isAnonymousClass  false                                                                                                                                                             
 isArray           false                                                                                                                                                             
 isLocalClass      false                                                                                                                                                             
 isMemberClass     true                                                                                                                                                              
 isPrimitive       false                                                                                                                                                             
 isSynthetic       false                                                                                                                                                             
 simple-name       Counter                                                                                                                                                           
 modifier          static                                                                                                                                                            
 annotation                                                                                                                                                                          
 interfaces                                                                                                                                                                          
 super-class       +-java.lang.Object                                                                                                                                                
 class-loader      +-sun.misc.Launcher$AppClassLoader@659e0bfd                                                                                                                       
                     +-sun.misc.Launcher$ExtClassLoader@758c1b43                                                                                                                     
 classLoaderHash   659e0bfd                                                                                                                                                          
 fields            modifierprivate,static                                                                                                                                            
                   type    java.util.concurrent.atomic.AtomicInt                                                                                                                     
                           eger                                                                                                                                                      
                   name    count                                                                                                                                                     
                   value   3065                                                                                                                                                      
                                                                                                                                                                                     

Affect(row-cnt:1) cost in 23 ms.
```
