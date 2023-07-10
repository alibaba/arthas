
### 查看所有线程信息

`thread`{{execute T2}}


### 查看具体线程的栈

查看线程ID 16的栈：

`thread 16`{{execute T2}}

### 查看CPU使用率top n线程的栈

参数`n`用来指定最忙的前N个线程并打印堆栈

`thread -n 3`{{execute T2}}

参数`i`用来指定cpu占比统计的采样间隔，单位为毫秒

查看5秒内的CPU使用率top n线程栈

`thread -n 3 -i 5000`{{execute T2}}


### 查找线程是否有阻塞

参数`b`用来指定找出当前阻塞其他线程的线程

`thread -b`{{execute T2}}
