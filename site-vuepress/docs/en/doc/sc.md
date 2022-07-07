sc
==

[`sc` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-sc)

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
|`[c:]`|The hash code of the special class's classLoader|
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |
|`[n:]`|Maximum number of matching classes with details (100 by default)|

> *class-patten* supports full qualified class name, e.g. com.taobao.test.AAA and com/taobao/test/AAA. It also supports the format of 'com/taobao/test/AAA', so that it is convenient to directly copy class name from the exception stack trace without replacing '/' to '.'. <br/><br/>
> `sc` turns on matching sub-class match by default, that is, `sc` will also search the sub classes of the target class too. If exact-match is desired, pls. use `options disable-sub-class true`.

### Usage

* Wildcards match search

  ```bash
  $ sc demo.*
  demo.MathGame
  Affect(row-cnt:1) cost in 55 ms.
  ```

* View class details

  ```bash
  $ sc -d demo.MathGame
  class-info        demo.MathGame
  code-source       /private/tmp/math-game.jar
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

* View class fields

  ```bash
  $ sc -d -f demo.MathGame
  class-info        demo.MathGame
  code-source       /private/tmp/math-game.jar
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
