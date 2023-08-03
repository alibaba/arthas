首先向`/tmp/a.txt`文件中写入`hello, world`：

`echo "hello, world" > /tmp/a.txt`{{execute T2}}

通过 [cat 命令](https://arthas.aliyun.com/doc/cat.html) 可以打印文件内容，和 linux 里的 cat 命令类似。

`cat /tmp/a.txt`{{execute T2}}

你可以看到`hello, world`被打印了出来。
