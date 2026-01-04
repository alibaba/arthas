# vmtool

::: tip
@since 3.5.1
:::

[`vmtool`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-vmtool)

`vmtool` 利用`JVMTI`接口，实现查询内存对象，强制 GC 等功能。

- [JVM Tool Interface](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html)

## 获取对象

```bash
$ vmtool --action getInstances --className java.lang.String --limit 10
@String[][
    @String[com/taobao/arthas/core/shell/session/Session],
    @String[com.taobao.arthas.core.shell.session.Session],
    @String[com/taobao/arthas/core/shell/session/Session],
    @String[com/taobao/arthas/core/shell/session/Session],
    @String[com/taobao/arthas/core/shell/session/Session.class],
    @String[com/taobao/arthas/core/shell/session/Session.class],
    @String[com/taobao/arthas/core/shell/session/Session.class],
    @String[com/],
    @String[java/util/concurrent/ConcurrentHashMap$ValueIterator],
    @String[java/util/concurrent/locks/LockSupport],
]
```

::: tip
通过 `--limit`参数，可以限制返回值数量，避免获取超大数据时对 JVM 造成压力。默认值是 10。
:::

## 指定 classloader name

```bash
vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext
```

## 指定 classloader hash

可以通过`sc`命令查找到加载 class 的 classloader。

```bash
$ sc -d org.springframework.context.ApplicationContext
 class-info        org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
 code-source       file:/private/tmp/demo-arthas-spring-boot.jar!/BOOT-INF/lib/spring-boot-1.5.13.RELEASE.jar!/
 name              org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
...
 class-loader      +-org.springframework.boot.loader.LaunchedURLClassLoader@19469ea2
                     +-sun.misc.Launcher$AppClassLoader@75b84c92
                       +-sun.misc.Launcher$ExtClassLoader@4f023edb
 classLoaderHash   19469ea2
```

然后用`-c`/`--classloader` 参数指定：

```bash
vmtool --action getInstances -c 19469ea2 --className org.springframework.context.ApplicationContext
```

## 指定返回结果展开层数

::: tip
`getInstances` action 返回结果绑定到`instances`变量上，它是数组。

通过 `-x`/`--expand` 参数可以指定结果的展开层次，默认值是 1。
:::

```bash
vmtool --action getInstances -c 19469ea2 --className org.springframework.context.ApplicationContext -x 2
```

## 执行表达式

::: tip
`getInstances` action 返回结果绑定到`instances`变量上，它是数组。可以通过`--express`参数执行指定的表达式。
:::

```bash
vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext --express 'instances[0].getBeanDefinitionNames()'
```

## 强制 GC

```bash
vmtool --action forceGc
```

- 可以结合 [`vmoption`](vmoption.md) 命令动态打开`PrintGC`开关。

## 分析堆内存占用

`heapAnalyze` 会从 GC Root 可达对象出发，统计各个类的实例数量与占用字节数，并输出占用最大的若干对象与类。

```bash
$ vmtool --action heapAnalyze --classNum 5 --objectNum 3
```

::: tip
通过 `--classNum` 参数指定展示的类数量，通过 `--objectNum` 参数指定展示的对象数量。
:::

## 分析对象引用链

`referenceAnalyze` 用于分析某个类的实例对象，并输出占用最大的若干对象及其引用回溯链（从对象回溯到 GC Root），用于辅助定位对象来源。

```bash
$ vmtool --action referenceAnalyze --className java.lang.String --objectNum 5 --backtraceNum 3
```

::: tip

- 通过 `--objectNum` 参数指定展示的对象数量
- 通过 `--backtraceNum` 参数指定回溯层数，设置为 `-1` 表示一直回溯到 root，设置为 `0` 表示不输出引用链
- `getInstances` 支持的 `--classLoaderClass` / `--classloader` 参数同样适用于 `referenceAnalyze`
  :::

## interrupt 指定线程

thread id 通过`-t`参数指定，可以使用 `thread`命令获取。

```bash
vmtool --action interruptThread -t 1
```

## glibc 释放空闲内存

Linux man page: [malloc_trim](https://man7.org/linux/man-pages/man3/malloc_trim.3.html)

```bash
vmtool --action mallocTrim
```

## glibc 内存状态

内存状态将会输出到应用的 stderr。Linux man page: [malloc_stats](https://man7.org/linux/man-pages/man3/malloc_stats.3.html)

```bash
vmtool --action mallocStats
```

输出到 stderr 的内容如下：

```
Arena 0:
system bytes     =     135168
in use bytes     =      74352
Total (incl. mmap):
system bytes     =     135168
in use bytes     =      74352
max mmap regions =          0
max mmap bytes   =          0
```
