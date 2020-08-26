jad
===

[`jad` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-jad)

> Decompile the specified classes.

`jad` helps to decompile the byte code running in JVM to the source code to assist you to understand the logic behind better.

* The decompiled code is syntax highlighted for better readability in Arthas console.
* It is possible that there's grammar error in the decompiled code, but it should not affect your interpretation.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|`[c:]`|hashcode of the class loader that loads the class|
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |
|`[E]`|turn on regex match while the default is wildcard match|

### Usage

#### Decompile `java.lang.String`

```java
$ jad java.lang.String

ClassLoader:

Location:


/*
* Decompiled with CFR 0_132.
*/
package java.lang;

import java.io.ObjectStreamField;
...
public final class String
implements Serializable,
Comparable<String>,
CharSequence {
    private final char[] value;
    private int hash;
    private static final long serialVersionUID = -6849794470754667710L;
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

    public String(byte[] arrby, int n, int n2) {
        String.checkBounds(arrby, n, n2);
        this.value = StringCoding.decode(arrby, n, n2);
    }
...
```

#### Print source only

By default, the decompile result will have the `ClassLoader` information. With the `--source-only` option, you can print only the source code. Conveniently used with the [mc](mc.md)/[redefine](redefine.md) commands.

```
$ jad --source-only demo.MathGame
/*
 * Decompiled with CFR 0_132.
 */
package demo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MathGame {
    private static Random random = new Random();
    public int illegalArgumentCount = 0;
...
```

#### Decompile the specified method

```java
$ jad demo.MathGame main

ClassLoader:
+-sun.misc.Launcher$AppClassLoader@3d4eac69
+-sun.misc.Launcher$ExtClassLoader@66350f69

Location:
/private/tmp/arthas-demo.jar

public static void main(String[] args) throws InterruptedException {
    MathGame game = new MathGame();
    do {
        game.run();
        TimeUnit.SECONDS.sleep(1L);
    } while (true);
}

Affect(row-cnt:1) cost in 228 ms.
```

#### Decompile with specified classLoader

> If the target class is loaded by multiple classloaders, `jad` outputs the `hashcode` of the corresponding classloaders, then you can re-run `jad` and specify `-c <hashcode>` to decompile the target class from the specified classloader.

```java
$ jad org.apache.log4j.Logger

Found more than one class for: org.apache.log4j.Logger, Please use jad -c hashcode org.apache.log4j.Logger
HASHCODE  CLASSLOADER
69dcaba4  +-monitor's ModuleClassLoader
6e51ad67  +-java.net.URLClassLoader@6e51ad67
            +-sun.misc.Launcher$AppClassLoader@6951a712
            +-sun.misc.Launcher$ExtClassLoader@6fafc4c2
2bdd9114  +-pandora-qos-service's ModuleClassLoader
4c0df5f8  +-pandora-framework's ModuleClassLoader

Affect(row-cnt:0) cost in 38 ms.
$ jad org.apache.log4j.Logger -c 69dcaba4

ClassLoader:
+-monitor's ModuleClassLoader

Location:
/Users/admin/app/log4j-1.2.14.jar

package org.apache.log4j;

import org.apache.log4j.spi.*;

public class Logger extends Category
{
    private static final String FQCN;

    protected Logger(String name)
    {
        super(name);
    }

...

Affect(row-cnt:1) cost in 190 ms.
```

For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.
