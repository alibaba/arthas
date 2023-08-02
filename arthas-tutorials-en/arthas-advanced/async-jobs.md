Asynchronous jobs in arthas. The idea is borrowed from [linux jobs](http://man7.org/linux/man-pages/man1/jobs.1p.html).

### Use & to run the command in the background, redirect the output

The job output can be redirect to the specified file by `>` or `>>`, and can be used together with `&`. By doing this, you can achieve running commands asynchronously.

To troubleshoot an issue that may occur at an uncertain time, we can run a monitoring command in the background and save the output to a log file. Use the following command:

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}' 'throwExp != null' >> a.log &`{{exec}}

This command will execute in the background, allowing you to continue running other commands in the console.

Next, visit [/user/0]({{TRAFFIC_HOST1_80}}/user/0).

Then, use `cat a.log`{{exec}} to see that the URL we just accessed threw an exception.

### List background jobs

If you want to list all background jobs, you can execute the jobs command and the results are as follows:

`jobs`{{execute T2}}

You can see that there is currently a background job executing:

job id is 10, `*` indicates that this job is created by the current session.

status is Stopped.

execution count is the number of executions, which have been executed 19 times since the start.

timeout date: timeout timestamp, when the time exceeds this timestamp, the job will be automatically timeout and exit.

### Stop job

If you want to stop background job, just `kill <job-id>`.
If you wish to bring the command to the foreground or background for continued execution, you can use the `fg` and `bg` commands.

### Note

- Support up to 8 commands at the same time to redirect the output to the log files.
- Do not open too many background jobs at the same time to avoid negative performance effect to the target JVM.
- If you do not want to stop the Arthas service and continue to perform background tasks, you can exit the Arthas console by executing `quit` command (`stop` command will stop the Arthas service)
