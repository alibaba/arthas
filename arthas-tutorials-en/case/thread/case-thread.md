
### View all thread information

`thread`{{execute T2}}


### View the stack of specific threads

View the stack of thread ID 16:

`thread 16`{{execute T2}}

### View the stack of CPU usage TOP N threads

`n` is used to specify the top number of busiest threads with stack traces printed:

`thread -n 3`{{execute T2}}

`i` is used to specify the interval to collect data to compute CPU ratios (ms)

View the CPU usage TOP N thread stack in 5 seconds

`thread -n 3 -i 5000`{{execute T2}}

### Find if the thread is blocked

`b` is used to specify to locate the thread blocking the others

`thread -b`{{execute T2}}
