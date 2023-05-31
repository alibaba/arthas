
The job output can be redirect to the specified file by > or >>, and can be used together with &. By doing this, you can achieve running commands asynchronously, for example:

`trace demo.MathGame primeFactors >> test.out &`{{execute T2}}

At this time, the trace command will be executed in the background, and the result will be output to the `test.out` file under the `working directory` of the application. You can continue to execute other commands. And you can view the command execution result in the file. You can execute the `pwd` command to view the `working directory` of the current application.

`pwd`{{execute T2}}

`cat test.out`{{execute T2}}

