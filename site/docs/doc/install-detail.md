# Arthas Install

## 快速安装

### 使用`arthas-boot3`（推荐）

下载`arthas-boot3.jar`，然后用`java -jar`的方式启动：

```bash
curl -O https://arthas.aliyun.com/arthas-boot3.jar
java -jar arthas-boot3.jar
```

打印帮助信息：

```bash
java -jar arthas-boot3.jar -h
```

- 如果下载速度比较慢，可以使用 aliyun 的镜像：

  ```bash
  java -jar arthas-boot3.jar --repo-mirror aliyun --use-http
  ```

### 使用`as3.sh`

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://arthas.aliyun.com/install3.sh | sh
```

上述命令会下载启动脚本文件 `as3.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在 shell 下面执行`./as3.sh`，就会进入交互界面。

也可以执行`./as3.sh -h`来获取更多参数信息。

## 全量安装

从 Github Releases 页下载

[https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

解压后，在文件夹里有`arthas-boot3.jar`，直接用`java -jar`的方式启动：

```bash
java -jar arthas-boot3.jar
```

打印帮助信息：

```bash
java -jar arthas-boot3.jar -h
```

## 手动安装

[手动安装](manual-install.md)

## 通过 rpm/deb 来安装

在 releases 页面下载 rpm/deb 包： https://github.com/alibaba/arthas/releases

### 安装 deb

```bash
sudo dpkg -i arthas*.deb
```

### 安装 rpm

```bash
sudo rpm -i arthas*.rpm
```

### deb/rpm 安装的用法

在安装后，可以直接执行：

```bash
as3.sh
```

## 通过 Cloud Toolkit 插件使用 Arthas

- [通过 Cloud Toolkit 插件使用 Arthas 一键诊断远程服务器](https://github.com/alibaba/arthas/issues/570)

## 卸载

- 在 Linux/Unix/Mac 平台

  删除下面文件：

  ```bash
  rm -rf ~/.arthas/
  rm -rf ~/logs/arthas
  ```

- Windows 平台直接删除 user home 下面的`.arthas`和`logs/arthas`目录
