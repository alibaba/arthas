# Arthas Install

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

- 如果下载速度比较慢，可以使用 aliyun 的镜像：

  ```bash
  java -jar arthas-boot.jar --repo-mirror aliyun
  ```

### 使用`as.sh`

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://arthas.aliyun.com/install.sh | sh
```

上述命令会下载启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在 shell 下面执行`./as.sh`，就会进入交互界面。

也可以执行`./as.sh -h`来获取更多参数信息。

`as.sh` 按以下顺序查找可用的 Java，选中后停止查找：

1. 环境变量 `JAVA_HOME`。
2. `/opt/taobao/java`（目录存在时）。
3. macOS 的 `/usr/libexec/java_home` 返回的目录。
4. `PATH` 中的 `java`，解析软链接后确定安装目录。
5. 运行中 Java 进程的安装目录（需要进程列表中包含完整的 `/bin/java` 路径）。

每个候选目录都需要包含可执行的 `bin/java`，并且 `java -version` 执行成功、版本可以识别。Java 8 及以下还需要 `lib/tools.jar`；如果 `JAVA_HOME` 指向 JRE 子目录，脚本会向上查找最多两级，选择包含 `tools.jar` 和可执行 `bin/java` 的 JDK。Java 9 及以上不需要 `tools.jar`。

有效的 `JAVA_HOME` 始终优先。已配置的目录失效时，脚本会输出原因并继续查找；全部候选不可用时，会在下载或 attach 之前退出。可以为单次启动指定 Java：

```bash
JAVA_HOME="/path/to/jdk" ./as.sh
```

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
as.sh
```

## 通过 Cloud Toolkit 插件使用 Arthas

- [通过 Cloud Toolkit 插件使用 Arthas 一键诊断远程服务器](https://github.com/alibaba/arthas/issues/570)

## 离线帮助文档

最新版本离线文档下载：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/doc/latest_version?mirror=aliyun)

## 卸载

- 在 Linux/Unix/Mac 平台

  删除下面文件：

  ```bash
  rm -rf ~/.arthas/
  rm -rf ~/logs/arthas
  ```

- Windows 平台直接删除 user home 下面的`.arthas`和`logs/arthas`目录

---

::: warning
如需诊断 jdk 6/7 应用，请点击[此处下载 arthas 3](https://arthas.aliyun.com/3.x/doc/install-detail.html)。
:::
