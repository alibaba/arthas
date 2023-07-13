

### 查看所有线程信息

`thread`{{execute T2}}


### 查看具体线程的栈

查看线程ID 16的栈：

`thread 16`{{execute T2}}

### 查看CPU使用率top n线程的栈

`thread -n 3`{{execute T2}}

查看5秒内的CPU使用率top n线程栈

`thread -n 3 -i 5000`{{execute T2}}


### 查找线程是否有阻塞

`thread -b`{{execute T2}}
