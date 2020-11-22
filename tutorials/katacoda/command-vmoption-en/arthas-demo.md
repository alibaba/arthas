



Download `arthas-demo.jar` and start with the `java -jar` command:

`wget https://arthas.aliyun.com/arthas-demo.jar
java -jar arthas-demo.jar`{{execute T1}}

`arthas-demo` is a very simple program that randomly generates integers, performs factorization, and prints the results.
If the generated random number is negative, a error message will be printed.

To make a contrast with using vmoption afterwards, now we use `Ctrl+c`{{execute interrupt}} and the program exit without printing any additional infomation.

Then we start `arthas-demo` again：

`java -jar arthas-demo.jar`{{execute T1}}
