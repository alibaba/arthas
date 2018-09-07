Arthas Install
=============

This document is for developers of recommonmark, it contans the API functions

## 安装

* Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

    > curl -L TODO/install.sh | sh

    上述命令将会下载的启动脚本文件 as.sh 到当前目录，你可以放在任何地方或将其加入到 $PATH 中；


* 对于 Windows 平台，你也可以下载最新的 zip 包，解压后运行 bin 目录下的 as.bat。

    下载地址：TODO （点击下载推荐的 Arthas 版本）

    Windows用户如果在cmd里不能正常显示颜色，可以使用这个软件：https://sourceforge.net/projects/conemu



## 卸载

* 在 Linux/Unix/Mac 平台

    删除下面文件就可以了。

    > rm -rf ~/.arthas/ ~/.arthas_history

* Windows平台直接删除zip包和解压的文件

## 手动安装

* [手动安装步骤](manual-install.md)