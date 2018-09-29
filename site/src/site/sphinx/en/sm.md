sm
==

Check the method profile of the loaded classes (abbreviated from *Search Method*).

F.Y.I `sm` only presents the methods declared in the current class while ignoring those declared in ancestors.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for class name|
|*method-pattern*|pattern for method name|
|[d]|print the details of the method|
|[E]|turn on regex matching while the default mode is wildcard matching|

### Usage

Check all methods declared:

```
$ sm demo.Demo$Counter
demo.Demo$Counter-><init>
demo.Demo$Counter->value
demo.Demo$Counter->increment
Affect(row-cnt:3) cost in 5 ms.
```

Check details of all the methods declared in the class:
```bash
$ sm demo.Demo$Counter -d
 declaring-class   demo.Demo$Counter                                                                                                                                                 
 constructor-name  <init>                                                                                                                                                            
 modifier                                                                                                                                                                            
 annotation                                                                                                                                                                          
 parameters                                                                                                                                                                          
 exceptions                                                                                                                                                                          

 declaring-class  demo.Demo$Counter                                                                                                                                                  
 method-name      value                                                                                                                                                              
 modifier         public,static,synchronized                                                                                                                                         
 annotation                                                                                                                                                                          
 parameters                                                                                                                                                                          
 return           int                                                                                                                                                                
 exceptions                                                                                                                                                                          

 declaring-class  demo.Demo$Counter                                                                                                                                                  
 method-name      increment                                                                                                                                                          
 modifier         public,static,synchronized                                                                                                                                         
 annotation                                                                                                                                                                          
 parameters                                                                                                                                                                          
 return           void                                                                                                                                                               
 exceptions                                                                                                                                                                          

Affect(row-cnt:3) cost in 16 ms.
```

Check the details of a specific method in the class:

```bash
$ sm demo.Demo$Counter -d increment
 declaring-class  demo.Demo$Counter                                                                                                                                                  
 method-name      increment                                                                                                                                                          
 modifier         public,static,synchronized                                                                                                                                         
 annotation                                                                                                                                                                          
 parameters                                                                                                                                                                          
 return           void                                                                                                                                                               
 exceptions                                                                                                                                                                          

Affect(row-cnt:1) cost in 14 ms.
```
