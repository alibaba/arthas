
If you want to list all background jobs, you can execute the jobs command and the results are as follows:

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

You can see that there is currently a background job executing:

job id is 10, `*` indicates that this job is created by the current session.

status is Stopped

execution count is the number of executions, which have been executed 19 times since the start.

timeout date: timeout timestamp, when the time exceeds this timestamp, the job will be automatically timeout and exit.