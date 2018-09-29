monitor
=======

Monitor methods calling stack traces.

**You should know:**

1. `monitor` is a persistent command, it never returns until you press `Ctrl+C` to manually stop it;
2. the server runs the jobs in the background;
3. injected monitoring code will become invalid automatically once the monitoring jobs being terminated;
4. in theory, Arthas will not change any original behaviors but if it does, please do not hesitate to start an [issue](https://github.com/alibaba/arthas/issues).

### Properties monitored

|Property|Specification|
|---:|:---|
|timestamp|date and time of the monitored moment|
|class|Java class|
|method|constructor or regular methods|
|total|calling times|
|success|success count|
|fail|failure count|
|rt|average Return Time|
|fail-rate|failure ratio|

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|[E]|turn on regex matching while the default is wildcard matching|
|[c:]|monitoring interval (s), 60 seconds by default|
|[n:]|threshold of execution times|

### Usage

```bash
$ monitor demo.Demo$Counter value -c 5 -n 5
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 59 ms.
 timestamp            class              method  total  success  fail  avg-rt(ms)  fail-rate                                                
---------------------------------------------------------------------------------------------                                               
 2018-09-29 19:03:56  demo.Demo$Counter  value   5      5        0     0.23        0.00%                                                    

 timestamp            class              method  total  success  fail  avg-rt(ms)  fail-rate                                                
---------------------------------------------------------------------------------------------                                               
 2018-09-29 19:04:01  demo.Demo$Counter  value   5      5        0     0.06        0.00%                                                    

 timestamp            class              method  total  success  fail  avg-rt(ms)  fail-rate                                                
---------------------------------------------------------------------------------------------                                               
 2018-09-29 19:04:06  demo.Demo$Counter  value   5      5        0     0.06        0.00%                                                    

 timestamp            class              method  total  success  fail  avg-rt(ms)  fail-rate                                                
---------------------------------------------------------------------------------------------                                               
 2018-09-29 19:04:11  demo.Demo$Counter  value   5      5        0     0.18        0.00%                                                    

 timestamp            class              method  total  success  fail  avg-rt(ms)  fail-rate                                                
---------------------------------------------------------------------------------------------                                               
 2018-09-29 19:04:16  demo.Demo$Counter  value   5      5        0     0.06        0.00%                                                    

Command execution times exceed limit: 5, so command will exit. You can set it with -n option.
```
