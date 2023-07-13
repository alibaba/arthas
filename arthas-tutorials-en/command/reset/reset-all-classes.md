
`trace demo.MathGame primeFactors`{{execute T2}}

Enter `Q`{{execute T2}} or `Ctrl+C` to exit

`reset`{{execute T2}}


```bash
$ trace demo.MathGame primeFactors
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 92 ms, listenerId: 3
---ts=2020-07-16 05:41:11;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    ---[0.523823ms] demo.MathGame:primeFactors()
        ---[0.004476ms] throw:java.lang.IllegalArgumentException() #46

---ts=2020-07-16 05:41:12;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@70dea4e
    ---[0.111285ms] demo.MathGame:primeFactors()

$ reset
Affect(class count: 1 , method count: 0) cost in 5 ms, listenerId: 0
```
