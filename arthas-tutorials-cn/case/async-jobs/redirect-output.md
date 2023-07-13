
可通过>或者>>将任务输出结果输出到指定的文件中，可以和&一起使用，实现arthas命令的后台异步任务。比如：

`trace demo.MathGame primeFactors >> test.out &`{{execute T2}}

这时 trace 命令会在后台执行，并且把结果输出到应用`工作目录`下面的`test.out`文件。可继续执行其他命令。并可查看文件中的命令执行结果。可以执行`pwd`命令查看当前应用的`工作目录`。

`pwd`{{execute T2}}

`cat test.out`{{execute T2}}

