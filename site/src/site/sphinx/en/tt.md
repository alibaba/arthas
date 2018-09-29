tt
==

Check the `parameters`, `return values` and `exceptions` of the methods at different times.

`watch` is a powerful command but due to its feasibility and complexity, it's quite hard to locate the issue effectively. 

In such difficulties, `tt` comes into play. 

With the help of `tt` (*TimeTunnel*), you can check the contexts of the methods at different times in execution history. 

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*expression*|expression to monitor|
|*condition-expression*|condition expression to filter|
|[d:]|delete the time fragment specified by index|
|[D]|delete all the time fragments|
|[E]|enable regular expression to match (wildcard matching by default)|
|[i:]|display the detailed information from specified time fragment|
|[l]|list all the time fragments|
|[M:]|upper size limit in bytes for the result (10 * 1024 * 1024 by default) |
|[n:]|threshold of execution times|
|[p:]|replay the time fragment specified by index|
|[s:]|search-expression, to search the time fragments by OGNL express|
|[t]|record the method invocation within time fragments|
|[w:]|watch the time fragment by OGNL expression|
|[x:]|the depth to print the specified property with default value: 1|
|#cost|time cost|

Tips:
1. `tt -t *Demo addTwoLists params[0].length==1` with different amounts of parameters;
2. `tt -t *Demo addTwoLists 'params[1].get(0) instanceof String'` with different types of parameters;
3. `tt -t *Demo addTwoLists params[0].get(0).equals("a")` with specified parameter.
  
Advanced:
* [Critical fields in expression](advice-class.md)
* [Special usage](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

#### Property

|Name|Specification|
|---|---|
|INDEX|the index for each call based on time|
|TIMESTAMP|time to invoke the method|
|COST(ms)|time cost of the method call|
|IS-RET|whether method exits with normal return|
|IS-EXP|whether method failed with exceptions|
|OBJECT|`hashCode()` of the object invoking the method|
|CLASS|class name of the object invoking the method|
|METHOD|method being invoked|

### Usage

Let's record the whole calling contexts:
  
```bash
$ tt -t -n 3 demo.Demo addTwoLists
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 101 ms.
 INDEX    TIMESTAMP            COST(ms)   IS-RET   IS-EXP   OBJECT          CLASS                           METHOD                          
--------------------------------------------------------------------------------------------------------------------------------------------
 1000     2018-09-30 01:42:47  0.169046   true     false    NULL            Demo                            addTwoLists                     
 1001     2018-09-30 01:42:47  0.156924   true     false    NULL            Demo                            addTwoLists                     
 1002     2018-09-30 01:42:47  0.07771    true     false    NULL            Demo                            addTwoLists                     
Command execution times exceed limit: 3, so command will exit. You can set it with -n option.

$ tt demo.Demo testListAdd -t -n 3
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 50 ms.
 INDEX    TIMESTAMP            COST(ms)   IS-RET   IS-EXP   OBJECT          CLASS                           METHOD                          
--------------------------------------------------------------------------------------------------------------------------------------------
 1003     2018-09-30 01:53:03  2.27651    true     false    NULL            Demo                            testListAdd                     
 1004     2018-09-30 01:53:03  0.088584   true     false    NULL            Demo                            testListAdd                     
 1005     2018-09-30 01:53:03  0.0783     true     false    NULL            Demo                            testListAdd                     
Command execution times exceed limit: 3, so command will exit. You can set it with -n option.
```


### Searching for records

#### All the recorded

```
$ tt -l
 INDEX    TIMESTAMP            COST(ms)   IS-RET   IS-EXP   OBJECT          CLASS                           METHOD                          
--------------------------------------------------------------------------------------------------------------------------------------------
 1000     2018-09-30 01:52:48  1.430461   true     false    NULL            Demo                            addTwoLists                     
 1001     2018-09-30 01:52:48  0.140251   true     false    NULL            Demo                            addTwoLists                     
 1002     2018-09-30 01:52:48  0.067769   true     false    NULL            Demo                            addTwoLists                     
 1003     2018-09-30 01:53:03  2.27651    true     false    NULL            Demo                            testListAdd                     
 1004     2018-09-30 01:53:03  0.088584   true     false    NULL            Demo                            testListAdd                     
 1005     2018-09-30 01:53:03  0.0783     true     false    NULL            Demo                            testListAdd                     
Affect(row-cnt:6) cost in 3 ms.

```

#### A specified method

```
$ tt -s method.name=="addTwoLists"
 INDEX    TIMESTAMP            COST(ms)   IS-RET   IS-EXP   OBJECT          CLASS                           METHOD                          
--------------------------------------------------------------------------------------------------------------------------------------------
 1000     2018-09-30 01:52:48  1.430461   true     false    NULL            Demo                            addTwoLists                     
 1001     2018-09-30 01:52:48  0.140251   true     false    NULL            Demo                            addTwoLists                     
 1002     2018-09-30 01:52:48  0.067769   true     false    NULL            Demo                            addTwoLists                     
Affect(row-cnt:6) cost in 3 ms.

```

Advanced:
* [Critical fields in expression](advice-class.md)

### Check context of the call

Using `tt -i <index>` to check a specific calling details.

```
$ tt -i 1000
 INDEX          1000                                                                                                                        
 GMT-CREATE     2018-09-30 01:52:48                                                                                                         
 COST(ms)       1.430461                                                                                                                    
 OBJECT         NULL                                                                                                                        
 CLASS          demo.Demo                                                                                                                   
 METHOD         addTwoLists                                                                                                                 
 IS-RETURN      true                                                                                                                        
 IS-EXCEPTION   false                                                                                                                       
 PARAMETERS[0]  @ArrayList[                                                                                                                 
                    @String[a],                                                                                                             
                    @String[b],                                                                                                             
                    @String[c],                                                                                                             
                    @String[d],                                                                                                             
                 ]                                                                                                                           
 PARAMETERS[1]  @ArrayList[                                                                                                                 
                    @String[c],                                                                                                             
                    @String[d],                                                                                                             
                 ]                                                                                                                           
 RETURN-OBJ     @Integer[4]                                                                                                                 
Affect(row-cnt:1) cost in 8 ms.
```

### Re-produce

Since Arthas stores the context of the call, you can even *replay* the method calling afterwards with extra option `-p` to re-produce the issue for advanced troubleshooting.

```
$ tt -i 1002 -p
 RE-INDEX       1002                                                                                                                        
 GMT-REPLAY     2018-09-30 02:00:27                                                                                                         
 OBJECT         NULL                                                                                                                        
 CLASS          demo.Demo                                                                                                                   
 METHOD         addTwoLists                                                                                                                 
 PARAMETERS[0]  @ArrayList[                                                                                                                 
                    @String[a],                                                                                                             
                    @String[b],                                                                                                             
                    @String[c],                                                                                                             
                    @String[d],                                                                                                             
                 ]                                                                                                                           
 PARAMETERS[1]  @ArrayList[                                                                                                                 
                    @String[c],                                                                                                             
                    @String[d],                                                                                                             
                 ]                                                                                                                           
 IS-RETURN      true                                                                                                                        
 IS-EXCEPTION   false                                                                                                                       
 RETURN-OBJ     @Integer[6]                                                                                                                 
Time fragment[1002] successfully replayed.
Affect(row-cnt:1) cost in 4 ms.
```

**You should know:**
1. **Loss** of the thread local variables will be a undeniable fact since there is no way for Arthas to record the thread local info (*If you find one, please share with us in [issues tracker](https://github.com/alibaba/arthas/issues)*). 
2. **Potential** modifications of objects can happen since only a reference will be recorded while later operations might modify objects without Arthas's watch.
