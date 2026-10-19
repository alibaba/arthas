# 批处理功能

::: tip
通过批处理功能，arthas 支持一次性批量运行多个命令，并取得命令执行的结果。可结合 `--select` 参数可以指定进程名字一起使用。
:::

## 使用方法

### 第一步：创建你的批处理脚本

这里我们新建了一个`test.as`脚本，为了规范，我们采用了.as 后缀名，但事实上任意的文本文件都 ok。

::: tip

- 目前需要每个命令占一行
- dashboard 务必指定执行次数 (`-n`)，否则会导致批处理脚本无法终止
- watch/tt/trace/monitor/stack 等命令务必指定执行次数 (`-n`)，否则会导致批处理脚本无法终止
  :::

```
➜  arthas git:(develop) cat /var/tmp/test.as
help
dashboard -n 1
session
thread
sc -d org.apache.commons.lang.StringUtils
```

批处理脚本也支持输出重定向和后台任务，`&` 写在命令末尾表示后台任务：

```bash
trace -n 1 Test t >> test.out
trace Test t >> test.out &
watch c.t.X test returnObj > &
```

第一条命令执行一次后结束，结果追加到 `test.out`。第二条命令在后台运行，结果同样追加到 `test.out`，批处理脚本继续执行下一条命令。第三条命令未指定重定向文件名，结果写入 `~/logs/arthas-cache/` 目录下的日志文件。

指定重定向的后台任务将结果写入文件；未指定重定向时，结果仍会打印在批处理输出中。`&` 需要是命令行的最后一个参数，批处理脚本不支持行尾注释。更多信息参考 [后台异步任务](async.md)。

### 第二步：运行你的批处理脚本

通过`-f`执行脚本文件，批处理脚本默认会输出到标准输出中，可以将结果重定向到文件中。

```bash
./as.sh -f /var/tmp/test.as <pid> > test.out # pid 可以通过 jps 命令查看
```

也可以通过 `-c` 来指定指行的命令，比如

```bash
./as.sh -c 'sysprop; thread' <pid> > test.out # pid 可以通过 jps 命令查看
```

### 第三步：查看运行结果

```bash
cat test.out
```
