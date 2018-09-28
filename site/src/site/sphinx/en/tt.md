tt
==

Check the `parameters`, `return values` and `exceptions` of the methods at different times.

`watch` is a powerful command but due to its feasibility and complexity, it's quite hard to locate the issue effectively. 

In such difficulties, `tt` comes into play. 

With the help of `tt` (*TimeTunnel*), you can check the contexts of the methods at different times in execution history. 

### Usage

Let's record the whole calling contexts:
  
  ```java
  $ tt -t -n 3 *Test print
  Press Ctrl+D or Ctrl+X to abort.
  Affect(class-cnt:1 , method-cnt:1) cost in 115 ms.
  +----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
  |    INDEX |            TIMESTAMP |   COST(ms) |   IS-RET |   IS-EXP |          OBJECT |                          CLASS |                         METHOD |
  +----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
  |     1007 |  2015-07-26 12:23:21 |        138 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
  +----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
  |     1008 |  2015-07-26 12:23:22 |        143 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
  +----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
  |     1009 |  2015-07-26 12:23:23 |        130 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
  +----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
  $ 
  ```

#### F.Y.I

  - `-t`

     record the calling context of the method `*Test.print`
  
  - `-n 3`

     limit the number of the records (avoid overflow for too many records; with `-n` option, Arthas can automatically stop recording once the records reach the specified limit)

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

#### Condition expression

Tips:
1. `tt -t *Test print params[0].length==1` with different amounts of parameters;
2. `tt -t *Test print 'params[1] instanceof Integer'` with different types of parameters;
3. `tt -t *Test print params[0].mobile=="13989838402"` with specified parameter.
  
Advanced:
* [Critical fields in expression](advice-class.md)
* [Special usage](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)


### Searching for records

#### All the recorded

```
$ tt -l
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|    INDEX |            TIMESTAMP |   COST(ms) |   IS-RET |   IS-EXP |          OBJECT |                          CLASS |                         METHOD |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1000 |  2015-07-26 01:16:27 |        130 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1001 |  2015-07-26 01:16:27 |          0 |    false |     true |      0x42cc13a0 |                GaOgnlUtilsTest |                   printAddress |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1002 |  2015-07-26 01:16:28 |        119 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1003 |  2015-07-26 01:16:28 |          0 |    false |     true |      0x42cc13a0 |                GaOgnlUtilsTest |                   printAddress |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1004 |  2015-07-26 12:21:56 |        130 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1005 |  2015-07-26 12:21:57 |        138 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1006 |  2015-07-26 12:21:58 |        130 |     true |    false |      0x42cc13a0 |                GaOgnlUtilsTest |                          print |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
Affect(row-cnt:7) cost in 2 ms.
$ 
```

#### A specified method

```
$ tt -s method.name=="printAddress"
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|    INDEX |            TIMESTAMP |   COST(ms) |   IS-RET |   IS-EXP |          OBJECT |                          CLASS |                         METHOD |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1001 |  2015-07-26 01:16:27 |          0 |    false |     true |      0x42cc13a0 |                GaOgnlUtilsTest |                   printAddress |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
|     1003 |  2015-07-26 01:16:28 |          0 |    false |     true |      0x42cc13a0 |                GaOgnlUtilsTest |                   printAddress |
+----------+----------------------+------------+----------+----------+-----------------+--------------------------------+--------------------------------+
Affect(row-cnt:2) cost in 55 ms.
$ 
```

Advanced:
* [Critical fields in expression](advice-class.md)

### Check context of the call

Using `tt -i <index>` to check a specific calling details.

```
$ 
$ tt -i 1003
+-----------------+------------------------------------------------------------------------------------------------------+
|           INDEX | 1003                                                                                                 |
+-----------------+------------------------------------------------------------------------------------------------------+
|      GMT-CREATE | 2015-07-26 01:16:28                                                                                  |
+-----------------+------------------------------------------------------------------------------------------------------+
|        COST(ms) | 0                                                                                                    |
+-----------------+------------------------------------------------------------------------------------------------------+
|          OBJECT | 0x42cc13a0                                                                                           |
+-----------------+------------------------------------------------------------------------------------------------------+
|           CLASS | GaOgnlUtilsTest                                                                                      |
+-----------------+------------------------------------------------------------------------------------------------------+
|          METHOD | printAddress                                                                                         |
+-----------------+------------------------------------------------------------------------------------------------------+
|       IS-RETURN | false                                                                                                |
+-----------------+------------------------------------------------------------------------------------------------------+
|    IS-EXCEPTION | true                                                                                                 |
+-----------------+------------------------------------------------------------------------------------------------------+
|   PARAMETERS[0] | Address@53448f87                                                                                     |
+-----------------+------------------------------------------------------------------------------------------------------+
| THROW-EXCEPTION | java.lang.RuntimeException: test                                                                     |
|                 |     at GaOgnlUtilsTest.printAddress(Unknown Source)                                                  |
|                 |     at GaOgnlUtilsTest.<init>(Unknown Source)                                                        |
|                 |     at GaOgnlUtilsTest.main(Unknown Source)                                                          |
+-----------------+------------------------------------------------------------------------------------------------------+
Affect(row-cnt:1) cost in 1 ms.
$ 
```

### Re-produce

Since Arthas stores the context of the call, you can even *replay* the method calling afterwards with extra option `-p` to re-produce the issue for advanced troubleshooting.

```
$ tt -i 1003 -p
+-----------------+---------------------------------------------------------------------------------------------------------+
|        RE-INDEX | 1003                                                                                                    |
+-----------------+---------------------------------------------------------------------------------------------------------+
|      GMT-REPLAY | 2015-07-26 17:29:51                                                                                     |
+-----------------+---------------------------------------------------------------------------------------------------------+
|          OBJECT | 0x42cc13a0                                                                                              |
+-----------------+---------------------------------------------------------------------------------------------------------+
|           CLASS | GaOgnlUtilsTest                                                                                         |
+-----------------+---------------------------------------------------------------------------------------------------------+
|          METHOD | printAddress                                                                                            |
+-----------------+---------------------------------------------------------------------------------------------------------+
|   PARAMETERS[0] | Address@53448f87                                                                                        |
+-----------------+---------------------------------------------------------------------------------------------------------+
|       IS-RETURN | false                                                                                                   |
+-----------------+---------------------------------------------------------------------------------------------------------+
|    IS-EXCEPTION | true                                                                                                    |
+-----------------+---------------------------------------------------------------------------------------------------------+
| THROW-EXCEPTION | java.lang.RuntimeException: test                                                                        |
|                 |     at GaOgnlUtilsTest.printAddress(GaOgnlUtilsTest.java:78)                                            |
|                 |     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)                                      |
|                 |     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)                    |
|                 |     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)            |
|                 |     at java.lang.reflect.Method.invoke(Method.java:483)                                                 |
|                 |     at com.github.ompc.Arthas.util.GaMethod.invoke(GaMethod.java:81)                                     |
|                 |     at com.github.ompc.Arthas.command.TimeTunnelCommand$6.action(TimeTunnelCommand.java:592)             |
|                 |     at com.github.ompc.Arthas.server.DefaultCommandHandler.execute(DefaultCommandHandler.java:175)       |
|                 |     at com.github.ompc.Arthas.server.DefaultCommandHandler.executeCommand(DefaultCommandHandler.java:83) |
|                 |     at com.github.ompc.Arthas.server.GaServer$4.run(GaServer.java:329)                                   |
|                 |     at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)                  |
|                 |     at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)                  |
|                 |     at java.lang.Thread.run(Thread.java:745)                                                            |
+-----------------+---------------------------------------------------------------------------------------------------------+
replay time fragment[1003] success.
Affect(row-cnt:1) cost in 3 ms.
$ 
```

F.Y.I
1. the calling stack is little different using Arthas now unlike the original;
2. **Loss** of the thread local variables will be a undeniable fact since there is no way for Arthas to record the thread local info (*If you find one, please share with us in [issues tracker](https://github.com/alibaba/arthas/issues)*). 
3. **Potential** modifications of objects can happen since only a reference will be recorded while later operations might modify objects without Arthas's watch.
