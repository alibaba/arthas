批处理功能
===

> 通过批处理功能，arthas支持一次性批量运行多个命令，并取得命令执行的结果。

### 使用方法

#### 第一步： 创建你的批处理脚本

这里我们新建了一个`test.as`脚本，为了规范，我们采用了.as后缀名，但事实上任意的文本文件都ok。

> 注意事项
> * 目前需要每个命令占一行
> * dashboard务必指定执行次数(`-n`)，否则会导致批处理脚本无法终止
> * watch/tt/trace/monitor/stack等命令务必指定执行次数(`-n`)，否则会导致批处理脚本无法终止
> * 可以使用异步后台任务，如 `watch c.t.X test returnObj > &`，让命令一直在后台运行，通过日志获取结果，[获取更多异步任务的信息](async.md)

```
➜  arthas git:(develop) cat /var/tmp/test.as
help
dashboard -n 1
session
thread
sc -d org.apache.commons.lang.StringUtils
```

#### 第二步： 运行你的批处理脚本

通过`-f`执行脚本文件， 批处理脚本默认会输出到标准输出中，可以将结果重定向到文件中。

```bash
./as.sh -f /var/tmp/test.as <pid> > test.out # pid 可以通过 jps 命令查看
```

也可以通过 `-c` 来指定指行的命令，比如

```bash
./as.sh -c 'sysprop; thread' <pid> > test.out # pid 可以通过 jps 命令查看
```

#### 第三步： 查看运行结果

```bash
cat test.out
```