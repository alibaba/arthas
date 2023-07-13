



Download `math-game.jar` and start with the `java -jar` command:

`wget https://arthas.aliyun.com/math-game.jar
java -jar math-game.jar`{{execute T1}}

`math-game` is a very simple program that randomly generates integers, performs factorization, and prints the results.
If the generated random number is negative, a error message will be printed.

To make a contrast with using vmoption afterwards, now we use `Ctrl+c`{{execute interrupt}} and the program exit without printing any additional infomation.

Then we start `math-game` again：

`java -jar math-game.jar`{{execute T1}}
