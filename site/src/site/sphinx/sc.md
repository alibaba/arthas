sc
===

[`sc`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-sc)

> 查看JVM已加载的类信息

“Search-Class” 的简写，这个命令能搜索出所有已经加载到 JVM 中的 Class 信息，这个命令支持的参数有 `[d]`、`[E]`、`[f]` 和 `[x:]`。

参数说明
---

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|[d]|输出当前类的详细信息，包括这个类所加载的原始文件来源、类的声明、加载的ClassLoader等详细信息。<br/>如果一个类被多个ClassLoader所加载，则会出现多次|
|[E]|开启正则表达式匹配，默认为通配符匹配|
|[f]|输出当前类的成员变量信息（需要配合参数-d一起使用）|
|[x:]|指定输出静态变量时属性的遍历深度，默认为 0，即直接使用 `toString` 输出|
|`[c:]`|指定class的 ClassLoader 的 hashcode|
|`[classLoaderClass:]`|指定执行表达式的 ClassLoader 的 class name|
|`[n:]`|具有详细信息的匹配类的最大数量（默认为100）|

> class-pattern支持全限定名，如com.taobao.test.AAA，也支持com/taobao/test/AAA这样的格式，这样，我们从异常堆栈里面把类名拷贝过来的时候，不需要在手动把`/`替换为`.`啦。

> sc 默认开启了子类匹配功能，也就是说所有当前类的子类也会被搜索出来，想要精确的匹配，请打开`options disable-sub-class true`开关

### 使用参考

* 模糊搜索

  ```bash
  $ sc demo.*
  demo.MathGame
  Affect(row-cnt:1) cost in 55 ms.
  ```

* 打印类的详细信息

  ```bash
  $ sc -d demo.MathGame
  class-info        demo.MathGame
  code-source       /private/tmp/arthas-demo.jar
  name              demo.MathGame
  isInterface       false
  isAnnotation      false
  isEnum            false
  isAnonymousClass  false
  isArray           false
  isLocalClass      false
  isMemberClass     false
  isPrimitive       false
  isSynthetic       false
  simple-name       MathGame
  modifier          public
  annotation
  interfaces
  super-class       +-java.lang.Object
  class-loader      +-sun.misc.Launcher$AppClassLoader@3d4eac69
                      +-sun.misc.Launcher$ExtClassLoader@66350f69
  classLoaderHash   3d4eac69

  Affect(row-cnt:1) cost in 875 ms.
  ```

* 打印出类的Field信息

  ```bash
  $ sc -d -f demo.MathGame
  class-info        demo.MathGame
  code-source       /private/tmp/arthas-demo.jar
  name              demo.MathGame
  isInterface       false
  isAnnotation      false
  isEnum            false
  isAnonymousClass  false
  isArray           false
  isLocalClass      false
  isMemberClass     false
  isPrimitive       false
  isSynthetic       false
  simple-name       MathGame
  modifier          public
  annotation
  interfaces
  super-class       +-java.lang.Object
  class-loader      +-sun.misc.Launcher$AppClassLoader@3d4eac69
                      +-sun.misc.Launcher$ExtClassLoader@66350f69
  classLoaderHash   3d4eac69
  fields            modifierprivate,static
                    type    java.util.Random
                    name    random
                    value   java.util.Random@522b4
                            08a

                    modifierprivate
                    type    int
                    name    illegalArgumentCount


  Affect(row-cnt:1) cost in 19 ms.
  ```