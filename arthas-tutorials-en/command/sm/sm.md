> Search method from the loaded classes.

`sm` stands for search method. This command can search and show method information from all loaded classes. `sm` can only view the methods declared on the target class, that is, methods from its parent classes are invisible.

### Options

|                  Name | Specification                                                      |
| --------------------: | :----------------------------------------------------------------- |
|       _class-pattern_ | pattern for class name                                             |
|      _method-pattern_ | pattern for method name                                            |
|                 `[d]` | print the details of the method                                    |
|                 `[E]` | turn on regex matching while the default mode is wildcard matching |
|                `[c:]` | The hash code of the special class's classLoader                   |
| `[classLoaderClass:]` | The class name of the ClassLoader that executes the expression.    |
|                `[n:]` | Maximum number of matching classes with details (100 by default)   |

[sm command Docs](https://arthas.aliyun.com/en/doc/sm.html)

### Usage

- View methods of `java.lang.String`:

`sm java.lang.String`{{execute T2}}

- Specify ClassLoader

Find ClassLoaderHash:

`sc -d demo.MathGame | grep classLoaderHash`{{execute T2}}

- Specify Classloader

Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.

For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`sm --classLoaderClass sun.misc.Launcher$AppClassLoader demo.MathGame`{{execute T2}}

- PS: Here the classLoaderClass in java 8 is sun.misc.Launcher$AppClassLoader, while in java 11 it's jdk.internal.loader.ClassLoaders$AppClassLoader. Currently katacoda using java 8.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

- View method `java.lang.String#toString` details:

`sm -d java.lang.String toString`{{execute T2}}
