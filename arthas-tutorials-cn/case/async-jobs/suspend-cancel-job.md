
当任务正在前台执行，比如直接调用命令`trace Test t`或者调用后台执行命令`trace Test t &`后又通过fg命令将任务转到前台。这时console中无法继续执行命令，但是可以接收并处理以下事件：

`ctrl + z`：将任务暂停。通过jbos查看任务状态将会变为Stopped，通过`bg <job-id>`或者`fg <job-id>`可让任务重新开始执行

`ctrl + c`：停止任务

`ctrl + d`：按照linux语义应当是退出终端，目前arthas中是空实现，不处理
