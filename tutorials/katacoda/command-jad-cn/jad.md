
> 反编译指定已加载类的源码

`jad` 命令将 JVM 中实际运行的 class 的 byte code 反编译成 java 代码，便于你理解业务逻辑；

* 在 Arthas Console 上，反编译出来的源码是带语法高亮的，阅读更方便
* 当然，反编译出来的 java 代码可能会存在语法错误，但不影响你进行阅读理解

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|`[c:]`|类所属 ClassLoader 的 hashcode|
|`[classLoaderClass:]`|指定执行表达式的 ClassLoader 的 class name|
|[E]|开启正则表达式匹配，默认为通配符匹配|

### 使用参考

#### 编译`java.lang.String`

`jad java.lang.String`{{execute T2}}

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

#### 反编译时只显示源代码

默认情况下，反编译结果里会带有`ClassLoader`信息，通过`--source-only`选项，可以只打印源代码。方便和`mc`/`redefine`命令结合使用。

`jad --source-only java.lang.String`{{execute T2}}

```
$ jad --source-only java.lang.String
...
        @Override
        public int compare(String string, String string2) {
            int n = string.length();
            int n2 = string2.length();
            int n3 = Math.min(n, n2);
            for (int i = 0; i < n3; ++i) {
                char c;
                char c2 = string.charAt(i);
                if (c2 == (c = string2.charAt(i)) || (c2 = Character.toUpperCase(c2)) == (c = Character.toUpperCase(c)) || (c2 = Character.toLowerCase(c2)) == (c = Character.toLowerCase(c))) continue;
                return c2 - c;
            }
            return n - n2;
        }

        private Object readResolve() {
            return String.CASE_INSENSITIVE_ORDER;
        }
    }
}
```

#### 反编译指定的函数

`jad java.lang.String toString`{{execute T2}}

```java
$ jad java.lang.String toString

ClassLoader:

Location:


@Override
public String toString() {
    return this;
}

Affect(row-cnt:2) cost in 407 ms.
```

#### 反编译时指定ClassLoader

> 当有多个 `ClassLoader` 都加载了这个类时，`jad` 命令会输出对应 `ClassLoader` 实例的 `hashcode`，然后你只需要重新执行 `jad` 命令，并使用参数 `-c <hashcode>` 就可以反编译指定 ClassLoader 加载的那个类了；

例如：

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
```

```java
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

对于只有唯一实例的ClassLoader还可以通过`--classLoaderClass`指定class name，使用起来更加方便：

`--classLoaderClass` 的值是ClassLoader的类名，只有匹配到唯一的ClassLoader实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。
