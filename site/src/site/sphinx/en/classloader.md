classloader
===========

Check the inheritance hierarchy, URLs and classes loading profiles of class loaders.

It can be a great help for `ResourceNotFoundException` when you can use command `classloader`to specify a class loader to get resources and print all the URLs of the valid resources.

### Options

|Name|Specification|
|---:|:---|
|[l]|list all class loader instances based on thread count|
|[t]|print the inheritance tree structure of class loaders|
|[a]|list all the classes loaded by all the class loaders (use it with great caution since the output can be huge)|
|[c:]|get the hashcode of the class loader|
|[r:]|using class loader to search resource (must be used with `-c`)|

### Usage

* Categorised by class loader

```bash
$ classloader 
 name                                       numberOfInstances  loadedCountTotal                                                                                         
 com.taobao.arthas.agent.ArthasClassloader  2                  3090                                              
 BootstrapClassLoader                       1                  1766                                            
 sun.misc.Launcher$AppClassLoader           1                  6                                              
 sun.reflect.DelegatingClassLoader          5                  5                                             
 sun.misc.Launcher$ExtClassLoader           1                  0                                            
Affect(row-cnt:5) cost in 11 ms.
```

* Categorized by class loader instance

```bash
$ classloader -l
 name                                                loadedCount  hash      parent                                               
 BootstrapClassLoader                                1766         null      null                                                 
 com.taobao.arthas.agent.ArthasClassloader@6d408cc2  1252         6d408cc2  sun.misc.Launcher$ExtClassLoader@758c1b43            
 com.taobao.arthas.agent.ArthasClassloader@25ae7a23  1840         25ae7a23  sun.misc.Launcher$ExtClassLoader@758c1b43            
 sun.misc.Launcher$AppClassLoader@659e0bfd           6            659e0bfd  sun.misc.Launcher$ExtClassLoader@758c1b43            
 sun.misc.Launcher$ExtClassLoader@758c1b43           0            758c1b43  null                                                 
Affect(row-cnt:5) cost in 8 ms.
```

* Check inheritance tree structure of class loaders

```shell
$ classloader -t
+-BootstrapClassLoader                                                                                                           
+-sun.misc.Launcher$ExtClassLoader@758c1b43                                                                                      
  +-com.taobao.arthas.agent.ArthasClassloader@6d408cc2                                                                           
  +-com.taobao.arthas.agent.ArthasClassloader@25ae7a23                                                                           
  +-sun.misc.Launcher$AppClassLoader@659e0bfd                                                                                    
Affect(row-cnt:5) cost in 7 ms.
```

* Check URL of the class loader

```shell
$ classloader -c 659e0bfd
file:/Users/hello/test/                                                                                                        
file:/Users/hello/.arthas/lib/3.0.5.20180922094548/arthas/arthas-agent.jar                                                     
                                                                                                                                 
Affect(row-cnt:8) cost in 6 ms.
```
* Using class loader to look for resources

```bash
$ classloader -c 659e0bfd -r demo/Demo.class
 file:/Users/hello/test/demo/Demo.class  
```
