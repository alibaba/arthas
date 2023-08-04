- _ID_: Java 级别的线程 ID，注意这个 ID 不能跟 jstack 中的 nativeID 一一对应

- _NAME_: 线程名

- _GROUP_: 线程组名

- _PRIORITY_: 线程优先级，1~10 之间的数字，越大表示优先级越高

- _STATE_: 线程的状态

- _CPU%_: 线程消耗的 cpu 占比，采样 100ms，将所有线程在这 100ms 内的 cpu 使用量求和，再算出每个线程的 cpu 使用占比。

- _TIME_: 线程运行总时间，数据格式为`分：秒`

- _INTERRUPTED_: 线程当前的中断位状态

- _DAEMON_: 是否是 daemon 线程

## 截图展示

![](https://arthas.aliyun.com/doc/_images/dashboard.png)
