sc
==

> Search classes loaded by JVM.

`sc` stands for search class. This command can search all possible classes loaded by JVM and show their information. The supported options are: `[d]`、`[E]`、`[f]` and `[x:]`.

### Supported Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|`[d]`|print the details of the current class, including its code source, class specification, its class loader and so on.<br/>If a class is loaded by more than one class loader, then the class details will be printed several times|
|`[E]`|turn on regex match, the default behavior is wildcards match|
|`[f]`|print the fields info of the current class, MUST be used with `-d` together|
|`[x:]`|specify the depth of recursive traverse the static fields, the default value is '0' - equivalent to use `toString` to output|

> *class-patten* supports full qualified class name, e.g. com.taobao.test.AAA and com/taobao/test/AAA. It also supports the format of 'com/taobao/test/AAA', so that it is convenient to directly copy class name from the exception stack trace without replacing '/' to '.'. <br/><br/>
> `sc` turns on matching sub-class match by default, that is, `sc` will also search the sub classes of the target class too. If exact-match is desired, pls. use `options disable-sub-class true`.

### Usage

For example, use `sc -df class-name` to view class's static fields:

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
