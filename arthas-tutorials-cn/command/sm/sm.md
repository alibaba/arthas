> 查看已加载类的方法信息

“Search-Method”的简写，这个命令能搜索出所有已经加载了 Class 信息的方法信息。

`sm` 命令只能看到由当前类所声明 (declaring) 的方法，父类则无法看到。

### 参数说明

|              参数名称 | 参数说明                                     |
| --------------------: | :------------------------------------------- |
|       _class-pattern_ | 类名表达式匹配                               |
|      _method-pattern_ | 方法名表达式匹配                             |
|                   [d] | 展示每个方法的详细信息                       |
|                   [E] | 开启正则表达式匹配，默认为通配符匹配         |
|                `[c:]` | 指定 class 的 ClassLoader 的 hashcode        |
| `[classLoaderClass:]` | 指定执行表达式的 ClassLoader 的 class name   |
|                `[n:]` | 具有详细信息的匹配类的最大数量（默认为 100） |

[sm 命令文档](https://arthas.aliyun.com/doc/sm.html)

### 使用参考

- 查找`java.lang.String`类的具体方法

`sm java.lang.String`{{execute T2}}

- 指定 ClassLoader

查找 ClassLoaderHash：

`sc -d demo.MathGame | grep classLoaderHash`{{execute T2}}

- 指定 classLoader

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。

如果你使用`-c`，你需要手动输入 hashcode：`-c <hashcode>`

对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：

`sm --classLoaderClass sun.misc.Launcher$AppClassLoader demo.MathGame`{{execute T2}}

- 注：这里 classLoaderClass 在 java 8 是 sun.misc.Launcher$AppClassLoader，而 java 11 的 classloader 是 jdk.internal.loader.ClassLoaders$AppClassLoader，katacoda 目前环境是 java8。

`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

- 查找`java.lang.String#toString`函数并打印详细信息

`sm -d java.lang.String toString`{{execute T2}}
