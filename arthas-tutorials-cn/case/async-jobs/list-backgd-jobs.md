
如果希望查看当前有哪些arthas任务在执行，可以执行jobs命令，执行结果如下

`jobs`{{execute T2}}

```bash
$ jobs
[1]*
       Running           trace demo.MathGame primeFactors &
       execution count : 49
       start time      : Wed Jul 22 05:47:52 GMT 2020
       timeout date    : Thu Jul 23 05:47:52 GMT 2020
       session         : aa75753d-74f1-4929-a829-7ff965345183 (current)
```

可以看到目前有一个后台任务在执行。

job id是10, `*` 表示此job是当前session创建

状态是Stopped

execution count是执行次数，从启动开始已经执行了19次

timeout date是超时的时间，到这个时间，任务将会自动超时退出