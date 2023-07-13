
Arthas 支持在 Linux/Unix/Mac 等平台上一键安装：

`curl -L https://arthas.aliyun.com/install.sh | sh`{{execute T2}}

上述命令会下载启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在shell下面执行`./as.sh`，就会进入交互界面。

也可以执行`./as.sh -h`来获取更多参数信息, 具体用法与`java -jar arthas-boot.jar`类似。