sc
===

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

> class-pattern支持全限定名，如com.taobao.test.AAA，也支持com/taobao/test/AAA这样的格式，这样，我们从异常堆栈里面把类名拷贝过来的时候，不需要在手动把`/`替换为`.`啦。

> sc 默认开启了子类匹配功能，也就是说所有当前类的子类也会被搜索出来，想要精确的匹配，请打开`options disable-sub-class true`开关

### 使用参考

例如， 查看类的静态变量信息， 可以用`sc -df class-name`

```shell
$ sc -df org.apache.commons.lang.StringUtils

 class-info        org.apache.commons.lang.StringUtils
 code-source       /Users/zhuyong/middleware/citrus-sample/petstore/web/target/petstore/WEB-INF/lib/commons-lang-2.4.jar
 name              org.apache.commons.lang.StringUtils
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       StringUtils
 modifier          public
 annotation
 interfaces
 super-class       +-java.lang.Object
 class-loader      +-com.taobao.tomcat.container.context.loader.AliWebappClassLoader
                     +-org.apache.catalina.loader.StandardClassLoader@1d44eef3
                       +-sun.misc.Launcher$AppClassLoader@57a462c9
                         +-sun.misc.Launcher$ExtClassLoader@6951a712
 fields             modifier  final,public,static
                    type      java.lang.String
                    name      EMPTY
                    value

                    modifier  final,public,static
                    type      int
                    name      INDEX_NOT_FOUND
                    value     -1

                    modifier  final,private,static
                    type      int
                    name      PAD_LIMIT
                    value     8192
```