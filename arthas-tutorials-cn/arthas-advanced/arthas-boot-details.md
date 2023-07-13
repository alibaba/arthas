

`arthas-boot.jar` 支持很多参数，可以执行 `java -jar arthas-boot.jar -h`{{execute T2}} 来查看。

## 允许外部访问

默认情况下， arthas server侦听的是 `127.0.0.1` 这个IP，如果希望远程可以访问，可以使用`--target-ip`的参数。

`java -jar arthas-boot.jar --target-ip`{{execute T2}}


## 列出所有的版本


`java -jar arthas-boot.jar --versions`{{execute T2}}

使用指定版本：

`java -jar arthas-boot.jar --use-version 3.1.0`{{execute T2}}

## 只侦听Telnet端口，不侦听HTTP端口

`java -jar arthas-boot.jar --telnet-port 9999 --http-port -1`{{execute T2}}

## 打印运行的详情

`java -jar arthas-boot.jar -v`{{execute T2}}



