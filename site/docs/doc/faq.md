# FAQ

::: tip
不在本列表里的问题，请到 issue 里搜索。 [https://github.com/alibaba/arthas/issues](https://github.com/alibaba/arthas/issues)
:::

### 日志文件在哪里？

日志文件路径： `~/logs/arthas/arthas.log`

### telnet: connect to address 127.0.0.1: Connection refused

1. 检查日志 `~/logs/arthas/arthas.log`
2. 检查`as.sh`/`arthas-boot.jar` 的启动参数，是否指定了特定的`port`
3. 用`netstat` 检查`LISTEN 3658` 端口的进程，确认它是`java`进程，并且是想要诊断的进程
4. 如果`LISTEN 3658` 端口的进程不是 `java` 进程，则`3658`端口已经被占用。需要在`as.sh`/`arthas-boot.jar` 的启动参数指定其它端口。
5. 确认进程和端口后，尝试用`telnet 127.0.0.1 3658`去连接

本质上`arthas`会在应用java进程内启动一个`tcp server`，然后使用`telnet`去连接它。

1. 可能端口不匹配
2. 可能进程本身已经挂起，不能接受新连接

如果Arthas 日志里有 `Arthas server already bind.`

1. 说明`Arthas server`曾经启动过，检查目标进程打开的文件描述符。如果是`linux`环境，可以去 `/proc/$pid/fd` 下面，使用`ls -alh | grep arthas`，检查进程是否已加载`arthas`相关的 jar 包。
2. 如果没有，那么可能已启动`arthas`的是其它进程，也可能应用已经重启过了。

### Arthas attach 之后对原进程性能有多大的影响

[https://github.com/alibaba/arthas/issues/44](https://github.com/alibaba/arthas/issues/44)

### target process not responding or HotSpot VM not loaded

com.sun.tools.attach.AttachNotSupportedException: Unable to open socket file: target process not responding or HotSpot VM not loaded

1. 检查当前用户和目标 java 进程是否一致。如果不一致，则切换到同一用户。JVM 只能 attach 同样用户下的 java 进程。
2. 尝试使用 `jstack -l $pid`，如果进程没有反应，则说明进程可能假死，无法响应 JVM attach 信号。所以同样基于 attach 机制的 Arthas 无法工作。尝试使用`jmap` heapdump 后分析。
3. 尝试按[quick-start](quick-start.md)里的方式 attach math-game。
4. 更多情况参考： [https://github.com/alibaba/arthas/issues/347](https://github.com/alibaba/arthas/issues/347)

### trace/watch 等命令能否增强 jdk 里的类？

默认情况下会过滤掉`java.`开头的类和被`BootStrap ClassLoader`加载的类。可以通过参数开启。

```bash
options unsafe true
```

更多参考 [options](options.md)

::: tip
通过 java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch append 到`Bootstrap ClassLoader`的 jar 包需要开启 unsafe。
:::

### 怎么以`json`格式查看结果

```bash
options json-format true
```

更多参考 [options](options.md)

### Arthas 能否跟踪 native 函数

不能。

### 能不能查看内存里某个变量的值

1. 可以使用[`vmtool`](vmtool.md)命令。
2. 可以用一些技巧，用[`tt`](tt.md)命令拦截到对象，或者从静态函数里取到对象。

### 方法同名过滤

同名方法过滤可以通过匹配表达式,可以使用[表达式核心变量](advice-class.md)中所有变量作为已知条件,可以通过判断参数个数`params.length ==1`, 参数类型`params[0] instanceof java.lang.Integer`、返回值类型 `returnObj instanceof java.util.List` 等等一种或者多种组合进行过滤。

可以使用 `-v` 查看观察匹配表达式的执行结果 [https://github.com/alibaba/arthas/issues/1348](https://github.com/alibaba/arthas/issues/1348)

例子[math-game](quick-start.md)

```bash
watch demo.MathGame primeFactors '{params,returnObj,throwExp}' 'params.length >0 && returnObj instanceof java.util.List' -v
```

### 怎么 watch、trace 构造函数 ？

```bash
watch demo.MathGame <init> '{params,returnObj,throwExp}' -v
```

### 怎样 watch、trace 内部类？

在 JVM 规范里内部类的格式是`OuterClass$InnerClass`。

```bash
watch OuterClass$InnerClass
```

### 是否支持 watch、trace lambda 类？

对于lambda生成的类，会跳过处理，因为 JVM 本身限制对 lambda 生成的类做增强。

- [https://github.com/alibaba/arthas/issues/1225](https://github.com/alibaba/arthas/issues/1225)

### 输入中文/Unicode 字符

把中文/Unicode 字符转为`\u`表示方法：

```bash
ognl '@java.lang.System@out.println("Hello \u4e2d\u6587")'
```

### java.lang.ClassFormatError: null、skywalking arthas 兼容使用

当出现这个错误日志`java.lang.ClassFormatError: null`,通常情况下都是被其他字节码工具修改过与 arthas 修改字节码不兼容。

比如: 使用 skywalking V8.1.0 以下版本 [无法 trace、watch 被 skywalking agent 增强过的类](https://github.com/alibaba/arthas/issues/1141), V8.1.0 以上版本可以兼容使用,更多参考 skywalking 配置 [skywalking compatible with other javaagent bytecode processing](https://github.com/apache/skywalking/blob/master/docs/en/FAQ/Compatible-with-other-javaagent-bytecode-processing.md#)。

#### class redefinition failed: attempted to change the schema (add/remove fields)

参考： [https://github.com/alibaba/arthas/issues/2165](https://github.com/alibaba/arthas/issues/2165)

### Arthas 能不能离线使用

可以。下载全量包解压即可，参考: [下载](download.md)。

### Arthas 怎么使用指定版本，不使用自动升级版本

1. 启动 `as.sh`/`arthas-boot.jar`时，可以用 `--use-version` 参数指定。
2. 下载全量包，解压后，`cd`到arthas目录启动，这种情况会使用当前目录下的版本。

### Attach docker/k8s 里的 pid 为 1 的进程失败

参考： [https://github.com/alibaba/arthas/issues/362#issuecomment-448185416](https://github.com/alibaba/arthas/issues/362#issuecomment-448185416)

### 为什么下载了新版本的 Arthas，连接的却是旧版本？

比如启动的 `as.sh/arthas-boot.jar` 版本是 3.5._ 的，但是连接上之后，打印的 arthas 版本是 3.4._ 的。

可能是之前使用旧版本的 arthas 诊断过目标进程。可以先执行`stop`停止掉旧版本的 arthas，再重新使用新版本 attach。

### 在ognl表达式中获取到 spring bean cglib 对象，但是 field 是 null

参考：

- [https://github.com/alibaba/arthas/issues/1802](https://github.com/alibaba/arthas/issues/1802)
- [https://github.com/alibaba/arthas/issues/1424](https://github.com/alibaba/arthas/issues/1424)
