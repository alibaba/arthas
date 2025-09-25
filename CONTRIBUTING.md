

## Issue

Welcome to use [issue tracker](https://github.com/alibaba/arthas/issues) to give us :bowtie::

* feedbacks - what you would like to have;
* usage tips - what usages you have found splendid;
* experiences - how you use Arthas to do **effective** troubleshooting;

## Documentation

* Under `site/docs/`.

## Online Tutorials

Please refer to [README.MD at killercoda branch](https://github.com/alibaba/arthas/tree/killercoda/README.md#contribution-guide)

## Developer

* Arthas runtime supports JDK6+
* It is recommended to use JDK8 to compile, and you will encounter problems when using a higher version. Reference https://github.com/alibaba/arthas/tree/master/.github/workflows
* If you encounter jfr related problems, it is recommended to use `8u262` and later versions of openjdk8 or zulu jdk8, https://mail.openjdk.org/pipermail/jdk8u-dev/2020-July/012143.html
### Local Installation

> Note: After modifying `arthas-vmtool` related codes, the packaging results need to be manually copied to the `lib/` path of this repo, and will not be copied automatically.

Recommend to use [`as-package.sh`](as-package.sh) to package, which will auto-install the latest Arthas to local `~/.arthas` and when debugging, Arthas will auto-load the latest version.

* To support jni, cpp compiling environment support is required
* mac needs to install xcode
* windows need to install gcc

F.Y.I
1. when using [`as.sh`](https://github.com/alibaba/arthas/blob/master/bin/as.sh) to start Arthas, it will get the latest version under `~/.arthas/lib`;
2. when [`as-package.sh`](as-package.sh) packaging, it will get the version from `pom.xml` and suffix it with the current timestamp e.g. `3.0.5.20180917161808`. 

You can also use `./mvnw clean package -DskipTests` to package and generate a `zip` under `packaging/target/` but remember when `as.sh` starts, it load the version under `~/.arthas/lib`.

### Start Arthas in specified version

When there are several different version, you can use `--use-version` to specify the version of Arthas to start your debug.

```bash
./as.sh --use-version 3.0.5.20180919185025
```

Tip: you can use `--versions` to list all available versions.

```bash
./as.sh --versions
```

### Debug

* [Debug Arthas In IDEA](https://github.com/alibaba/arthas/issues/222)

### Packaging All

* when packaging the whole project (Packaging All), you need to execute:

    ```bash
    ./mvnw clean package -DskipTests -P full
    ```

---



## Issue

欢迎在issue里对arthas做反馈，分享使用技巧，排查问题的经历。

* https://github.com/alibaba/arthas/issues

## 改进用户文档

用户文档在`site/docs/`目录下，如果希望改进arthas用户文档，欢迎提交PR。

## 改进在线教程

请参考[killercoda 分支下的说明](https://github.com/alibaba/arthas/tree/killercoda/README_CN.md#贡献指南)

## 开发者相关

* Arthas运行支持JDK6+
* 建议使用JDK8来编译，使用高版本会遇到问题。参考 https://github.com/alibaba/arthas/tree/master/.github/workflows
* 如果遇到jfr相关问题，建议使用`8u262`及之后的高版本 openjdk8 或者zulu jdk8， https://mail.openjdk.org/pipermail/jdk8u-dev/2020-July/012143.html
### 安装到本地

> 注意： 修改`arthas-vmtool`相关代码后，打包结果需要手动复制到本仓库的 `lib/` 路径下，不会自动复制。

本地开发时，推荐执行`as-package.sh`来打包，会自动安装最新版本的arthas到`~/.arthas`目录里。debug时会自动使用最新版本。

* 代码里要编译jni，需要cpp编译环境支持
* mac需要安装xcode
* windows需要安装gcc


`as.sh`在启动时，会对`~/.arthas/lib`下面的目录排序，取最新的版本。`as-package.sh`在打包时，会取`pom.xml`里的版本号，再拼接上当前时间，比如： `3.0.5.20180917161808`，这样子排序时取的就是最新的版本。

也可以直接 `./mvnw clean package -DskipTests`打包，生成的zip在 `packaging/target/` 下面。但是注意`as.sh`启动加载的是`~/.arthas/lib`下面的版本。

### 启动指定版本的arthas

本地开发时，可能会产生多个版本，可以用 `--use-version` 参数来指定版本，比如

```bash
./as.sh --use-version 3.0.5.20180919185025
```

可以用`--versions`参数来列出所有版本：

```bash
./as.sh --versions
```

### Debug

* [Debug Arthas In IDEA](https://github.com/alibaba/arthas/issues/222)

### 全量打包


* 全量打包时，需要配置下面的参数：

    ```
    ./mvnw clean package -DskipTests -P full
    ```

### Release Steps

发布release版本流程：

* 如果 arthas-vmtool 有更新，则需要手动触发action，构建后会把新的动态库文件提交到 lib 目录。 https://github.com/alibaba/arthas/actions/workflows/build-vmtool.yaml
* 修改`as.sh`里的版本，最后修改日期， `Bootstrap.java`里的版本，Dockerfile里的版本
* 修改本地的maven settings.xml
* 执行一次 gpg --sign /tmp/2.txt ，让 gpg 后台进程启动，否则打包可能失败
* mvn clean deploy -DskipTests -P full -P release

* 到 https://oss.sonatype.org/ 上，“Staging Repositories”然后close掉自己的，再release
* 发布后，可以到这里查看是否同步到仓库里了： https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/
* 发布完maven仓库之后，需要到阿里云的仓库里检查是否同步，有可能有延时

    比如下载地址： https://maven.aliyun.com/repository/public/com/taobao/arthas/arthas-packaging/3.x.x/arthas-packaging-3.x.x-bin.zip
    
    版本号信息地址： https://maven.aliyun.com/repository/public/com/taobao/arthas/arthas-packaging/maven-metadata.xml

* 打上tag，push tag到仓库上
* 需要更新 gh-pages 分支下面的 arthas-boot.jar/math-game.jar/as.sh ，下载 doc.zip，解压覆盖掉文档的更新，可以通过 github action 更新： https://github.com/alibaba/arthas/actions/workflows/update-doc.yaml
* 需要更新docker镜像，push新的tag：https://hub.docker.com/r/hengyunabc/arthas/tags?page=1&ordering=last_updated

    可以通过 github action push： https://github.com/alibaba/arthas/actions/workflows/push-docker.yaml 

* 更新README.md，比如增加了新命令，要加上说明，更新wiki的链接
* 更新release页面的 issue信息，修改信息等
* 更新 https://arthas.aliyun.com/api/latest_version api
* 更新内部的版本
