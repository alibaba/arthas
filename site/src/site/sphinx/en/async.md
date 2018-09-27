Arthas Async Jobs
===

Asynchronous jobs in arthas, using commands related to linux jobs.[linux man jobs](http://man7.org/linux/man-pages/man1/jobs.1p.html)。


## 1. Use & to run the command in the background

For example, execute the trace command in the background:

```sh
trace Test t &  
```

## 2. List background jobs

If you want to list all background jobs, you can execute the `jobs` command and the results are as follows:


```sh
$ jobs
[10]*
       Stopped           watch com.taobao.container.Test test "params[0].{? #this.name == null }" -x 2
       execution count : 19
       start time      : Fri Sep 22 09:59:55 CST 2017
       timeout date    : Sat Sep 23 09:59:55 CST 2017
       session         : 3648e874-5e69-473f-9eed-7f89660b079b (current)
```

You can see that there is currently a background job executing.

* job id is 10, `*` indicates that this job is created by the current session.
* status is `Stopped`
* execution count is the number of executions, which have been executed 19 times since the start.
* timeout date: After this time, the job will automatically timeout and exit.

## 3. Suspend and Cannel job

When the job is executing in the foreground, such as directly calling the command `trace Test t` or calling the background job command `trace Test t &`, the job is transferred to the foreground through the `fg` command. At this point, the console cannot continue to execute the command, but can receive and process the following keyboard events:

* ‘ctrl + z’: Suspend the job, the job status will change to `Stopped`, and the job can be restarted by `bg <job-id>` or `fg <job-id>`
* ‘ctrl + c’: Stop the job
* ‘ctrl + d’: According to linux semantics should be the exit terminal, currently arthas ignore this input.



## 4. fg/bg, Bring a background job to the foreground/Restart a stopped background job

* When the job is executed in the background or suspended (`ctrl + z` to suspend job), executing `fg <job-id>` will transfer the corresponding job to the foreground to continue execution. 
* When the job is suspended (`ctrl + z` to suspend job), executing `bg <job-id>` will continue the corresponding job in the background.
* A job created by a non-current session can only be executed by the current session fg to the foreground.

## 5. Redirect the job output

The job output can be redirect to the specified file by `>` or `>>`, and can be used together with `&` to implement the asynchronous job of the arthas command. such as:

```sh
$ trace Test t >> test.out &
```

The trace command will be executed in the background and the output will be redirect to `~/logs/arthas-cache/test.out`. You can continue to execute other commands in the console.


When connecting to a remote arthas server, you may not be able to view the files of the remote machine. Arthas also supports automatic redirection to the local cache file. Examples are as follows:

```sh
$ trace Test t >>  &
job id  : 2
cache location  : /Users/gehui/logs/arthas-cache/28198/2
```

You can see that does not specify the redirect file after `>>`, arthas will automatically redirect the job output to the `~/logs/arthas-cache`.

In the above example, pid is `28198` and job id is `2`.

## 6. Stop job

If you want to stop background job, just `kill <job-id>`.

## 7. Others

* Support up to 8 commands at the same time redirect the output to the log file.
* Do not open too many background asynchronous commands at the same time to avoid affecting the performance of the target JVM.