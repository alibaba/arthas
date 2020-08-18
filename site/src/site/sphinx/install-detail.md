Arthas Install
=============

## 快速安装

### 使用`arthas-boot`（推荐）

下载`arthas-boot.jar`，然后用`java -jar`的方式启动：

```bash
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar
```

打印帮助信息：

```bash
java -jar arthas-boot.jar -h
```

* 如果下载速度比较慢，可以使用aliyun的镜像：

    ```bash
    java -jar arthas-boot.jar --repo-mirror aliyun --use-http
    ```

### 使用`as.sh`

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://arthas.aliyun.com/install.sh | sh
```

上述命令会下载启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在shell下面执行`./as.sh`，就会进入交互界面。

也可以执行`./as.sh -h`来获取更多参数信息。

## 全量安装

最新版本，点击下载：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/latest_version?mirror=aliyun)

解压后，在文件夹里有`arthas-boot.jar`，直接用`java -jar`的方式启动：

```bash
java -jar arthas-boot.jar
```

打印帮助信息：

```bash
java -jar arthas-boot.jar -h
```


## 手动安装

[手动安装](manual-install.md)



## 通过rpm/deb来安装

在releases页面下载rpm/deb包： https://github.com/alibaba/arthas/releases

### 安装deb

```bash
sudo dpkg -i arthas*.deb
```
### 安装rpm

```bash
sudo rpm -i arthas*.rpm
```

### deb/rpm安装的用法

在安装后，可以直接执行：

```bash
as.sh
```

## 通过Cloud Toolkit插件使用Arthas

* [通过Cloud Toolkit插件使用Arthas一键诊断远程服务器](https://github.com/alibaba/arthas/issues/570)

## 离线帮助文档

最新版本离线文档下载：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/doc/latest_version?mirror=aliyun)

## 卸载

* 在 Linux/Unix/Mac 平台

    删除下面文件：
    ```bash
    rm -rf ~/.arthas/
    rm -rf ~/logs/arthas
    ```

* Windows平台直接删除user home下面的`.arthas`和`logs/arthas`目录
