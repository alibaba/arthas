Arthas Install
=============


## Linux/Unix/Mac

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

上述命令将会下载的启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 $PATH 中。


## Windows

下载最新的 zip 包，解压后运行 bin 目录下的 `as.bat`。

下载地址：[arthas-packaging-3.0.0-RC-bin.zip](https://search.maven.org/remotecontent?filepath=com/taobao/arthas/arthas-packaging/3.0.0-RC/arthas-packaging-3.0.0-RC-bin.zip)

Windows用户如果在cmd里不能正常显示颜色，可以使用[conemu](https://sourceforge.net/projects/conemu)。

## 独立的帮助文档

下载地址：[arthas-packaging-3.0.0-RC-doc.zip](https://search.maven.org/remotecontent?filepath=com/taobao/arthas/arthas-packaging/3.0.0-RC/arthas-packaging-3.0.0-RC-doc.zip)


## 卸载

* 在 Linux/Unix/Mac 平台

    删除下面文件：
    ```bash
    rm -rf ~/.arthas/ ~/.arthas_history
    ```

* Windows平台直接删除zip包和解压的文件
