
The job output can be redirect to the specified file by > or >>, and can be used together with &. By doing this, you can achieve running commands asynchronously, for example:

`trace demo.MathGame primeFactors >> test.out &`{{execute T2}}

The trace command will be running in the background and the output will be redirect to `~/logs/arthas-cache/test.out`. You can continue to execute other commands in the console, at the same time, you can also examine the execution result from the output file.

`cat test.out`{{execute T2}}

When connected to a remote Arthas server, you may not be able to view the output file on the remote machine. In this case, Arthas also supports automatically redirecting the output to the local cache file. Examples are as follows:

`trace demo.MathGame primeFactors >> &`{{execute T2}}

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/gehui/logs/arthas-cache/28198/2
```

If output path is not given, Arthas will automatically redirect the output to the local cache. `Job id` and `cache location` will be shown on the console. `Cache location` is a directory where the output files are put. For one given job, the path of its output file contains `PID` and `job id` in order to avoid potential conflict with other jobs. In the above example, `pid` is 28198 and `job id` is 2.
