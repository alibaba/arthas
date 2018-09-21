sc
==

Check the profiles of the loaded classes.

Abbreviated from “Search-Class”; with the help of this command, you can search out all the loaded classes in JVM. Supported options are: `[d]`、`[E]`、`[f]` and `[x:]`.

Options
-------

### Specification

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|[d]|print the details of the current class including the source file, class declaration, the class loaders and the like.<br/>F.Y.I if a class is loaded by several class loaders, then the class will be printed several times|
|[E]|turn on regx matching while the default is wildcards matching|
|[f]|print the fields info of the current class, which should be used along with `-d`|
|[x:]|the depth to print the static fields, whose default is `0` - directly invoke the `toString()`|

Tip: 
1. *class-patten* supports full qualified class name (e.g. com.taobao.test.AAA and com/taobao/test/AAA) 
2. `sc` turned on the `sub-class` matching in default mode, if you do want to hide the `sub-class` please just turn it off via `options disable-sub-class true`.

### Usage

Check the static fields of a class using `sc -df class-name`

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
