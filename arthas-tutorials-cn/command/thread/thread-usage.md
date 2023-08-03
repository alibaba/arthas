### 支持一键展示当前最忙的前 N 个线程并打印堆栈：

`thread -n 3`{{execute T2}}

- 没有线程 ID，包含`[Internal]`表示为 JVM 内部线程，参考`dashboard`命令的介绍。
- `cpuUsage`为采样间隔时间内线程的 CPU 使用率，与`dashboard`命令的数据一致。
- `deltaTime`为采样间隔时间内线程的增量 CPU 时间，小于 1ms 时被取整显示为 0ms。
- `time` 线程运行总 CPU 时间。

注意：线程栈为第二采样结束时获取，不能表明采样间隔时间内该线程都是在处理相同的任务。建议间隔时间不要太长，可能间隔时间越大越不准确。
可以根据具体情况尝试指定不同的间隔时间，观察输出结果。

### 当没有参数时，显示第一页线程信息

默认按照 CPU 增量时间降序排列，只显示第一页数据，避免滚屏。

`thread`{{execute T2}}

### thread --all, 显示所有匹配的线程

显示所有匹配线程信息，有时需要获取全部 JVM 的线程数据进行分析。

`thread --all`{{execute T2}}

### thread id，显示指定线程的运行堆栈

查看线程 ID 16 的栈：

`thread 16`{{execute T2}}

### thread -b, 找出当前阻塞其他线程的线程

有时候我们发现应用卡住了，通常是由于某个线程拿住了某个锁，并且其他线程都在等待这把锁造成的。为了排查这类问题，arthas 提供了`thread -b`，一键找出那个罪魁祸首。

`thread -b`{{execute T2}}

**注意**，目前只支持找出 synchronized 关键字阻塞住的线程，如果是`java.util.concurrent.Lock`，目前还不支持。

### thread -i, 指定采样时间间隔

- `thread -i 1000` : 统计最近 1000ms 内的线程 CPU 时间。

`thread -i 1000`{{execute T2}}

- `thread -n 3 -i 1000` : 列出 1000ms 内最忙的 3 个线程栈

`thread -n 3 -i 1000`{{execute T2}}

### thread –state，查看指定状态的线程

`thread --state WAITING`{{execute T2}}
