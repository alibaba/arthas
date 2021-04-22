



下载`math-game.jar`，再用`java -jar`命令启动：

`wget https://arthas.aliyun.com/math-game.jar
java -jar math-game.jar`{{execute T1}}

`math-game`是一个很简单的程序，它随机生成整数，再执行因式分解，把结果打印出来。如果生成的随机数是负数，则会打印提示信息。

为了和使用vmoption后的效果作对比，此时使用`Ctrl+c`{{execute interrupt}}，程序很自然地退出。

再次启动`math-game`：

`java -jar math-game.jar`{{execute T1}}
