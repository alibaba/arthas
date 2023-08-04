> Search classes loaded by JVM.

`sc` stands for search class. This command can search all possible classes loaded by JVM and show their information. The supported options are: `[d]`、`[E]`、`[f]` and `[x:]`.

### Supported Options

|                  Name | Specification                                                                                                                                                                                                                    |
| --------------------: | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|       _class-pattern_ | pattern for the class name                                                                                                                                                                                                       |
|      _method-pattern_ | pattern for the method name                                                                                                                                                                                                      |
|                 `[d]` | print the details of the current class, including its code source, class specification, its class loader and so on.<br/>If a class is loaded by more than one class loader, then the class details will be printed several times |
|                 `[E]` | turn on regex match, the default behavior is wildcards match                                                                                                                                                                     |
|                 `[f]` | print the fields info of the current class, MUST be used with `-d` together                                                                                                                                                      |
|                `[x:]` | specify the depth of recursive traverse the static fields, the default value is '0' - equivalent to use `toString` to output                                                                                                     |
|                `[c:]` | The hash code of the special class's classLoader                                                                                                                                                                                 |
| `[classLoaderClass:]` | The class name of the ClassLoader that executes the expression.                                                                                                                                                                  |
|                `[n:]` | Maximum number of matching classes with details (100 by default)                                                                                                                                                                 |

[sc command Docs](https://arthas.aliyun.com/en/doc/sc.html)

> _class-patten_ supports full qualified class name, e.g. com.taobao.test.AAA and com/taobao/test/AAA. It also supports the format of 'com/taobao/test/AAA', so that it is convenient to directly copy class name from the exception stack trace without replacing '/' to '.'. <br/><br/> > `sc` turns on matching sub-class match by default, that is, `sc` will also search the sub classes of the target class too. If exact-match is desired, pls. use `options disable-sub-class true`.

### Usage

- Wildcards match search `sc demo.*`{{execute T2}}
- View class details `sc -d demo.MathGame`{{execute T2}}

- Specify classLoader
  Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.  
  if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.  
  For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`sc --classLoaderClass sun.misc.Launcher$AppClassLoader -d demo*`{{execute T2}}

- PS: Here the classLoaderClass in java 8 is sun.misc.Launcher$AppClassLoader, while in java 11 it's jdk.internal.loader.ClassLoaders$AppClassLoader. Currently katacoda using java 8.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

- View class fields `sc -d -f demo.MathGame`{{execute T2}}
