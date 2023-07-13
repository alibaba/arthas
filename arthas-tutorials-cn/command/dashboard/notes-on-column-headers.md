
* *ID*: Java级别的线程ID，注意这个ID不能跟jstack中的nativeID一一对应

* *NAME*: 线程名

* *GROUP*: 线程组名

* *PRIORITY*: 线程优先级, 1~10之间的数字，越大表示优先级越高

* *STATE*: 线程的状态

* *CPU%*: 线程消耗的cpu占比，采样100ms，将所有线程在这100ms内的cpu使用量求和，再算出每个线程的cpu使用占比。

* *TIME*: 线程运行总时间，数据格式为`分：秒`

* *INTERRUPTED*: 线程当前的中断位状态

* *DAEMON*: 是否是daemon线程

## 截图展示

![](https://arthas.aliyun.com/doc/_images/dashboard.png)