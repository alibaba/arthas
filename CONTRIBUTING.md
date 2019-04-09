

## Issue

Welcome to use [issue tracker](https://github.com/alibaba/arthas/issues) to give us :bowtie::

* feedbacks - what you would like to have;
* usage tips - what usages you have found splendid;
* experiences - how you use Arthas to do **effective** troubleshooting;

## Documentation

Welcome PR to further improve English [documentation](https://github.com/alibaba/arthas/tree/master/site/src/site/sphinx/en).

## Developer

* Arthas runtime supports JDK6+
* To build Arthas requires JDK7+, because of the source code import JDK7 classes, such as `java.lang.management.BufferPoolMXBean`.


### Local Installation

Recommend to use [`as-package.sh`](as-package.sh) to package, which will auto-install the latest Arthas to local `~/.arthas` and when debugging, Arthas will auto-load the latest version.

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

* Arthas is using [Sphinx](http://www.sphinx-doc.org/en/master/) to generate the static site
* `sphinx-maven-plugin` configured in [`site/pom.xml`](https://github.com/alibaba/arthas/tree/master/site)
* `sphinx-maven-plugin` executes by downloading`sphinx-binary/`
* a [bug](https://github.com/rtfd/recommonmark/issues/93) in Sphinx plugin `recommonmark`; we fix it by packaging another [version](https://github.com/hengyunabc/sphinx-binary/releases/tag/v0.4.0.1)
* when packaging the whole project (Packaging All), you need to (Only Unix/Linux/Mac supported):

    ```bash
    ./mvnw clean package -DskipTests -P full -Dsphinx.binUrl=https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64
    ```


---



## issue

欢迎在issue里对arthas做反馈，分享使用技巧，排查问题的经历。

* https://github.com/alibaba/arthas/issues

## 改进用户文档

用户文档在`site/src/site/sphinx`目录下，如果希望改进arthas用户文档，欢迎提交PR。

英文文档在`site/src/site/sphinx/en`目录下，欢迎提交翻译PR。

## 开发者相关

* Arthas运行支持JDK6+
* 编译Arthas要求JDK7+，因为使用到了jdk7里的`java.lang.management.BufferPoolMXBean`。

### 安装到本地

本地开发时，推荐执行`as-package.sh`来打包，会自动安装最新版本的arthas到`~/.arthas`目录里。debug时会自动使用最新版本。

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

* arthas是用sphinx来生成静态网站
* 在`site/pom.xml`里配置了`sphinx-maven-plugin`
* `sphinx-maven-plugin`通过下载`sphinx-binary/`来执行
* sphinx配置的`recommonmark`插件有bug：https://github.com/rtfd/recommonmark/issues/93 ，因此另外打包了一个修复版本： https://github.com/hengyunabc/sphinx-binary/releases/tag/v0.4.0.1
* 全量打包时，需要配置下面的参数（目前只支持mac/linux）：

    ```
    ./mvnw clean package -DskipTests -P full -Dsphinx.binUrl=https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64
    ```

### Release Steps

发布release版本流程：

* 修改`as.sh`里的版本
* 修改本地的maven settings.xml
* mvn release:prepare -Darguments="-DskipTests"
* mvn release:perform -Darguments="-DskipTests -P full -Dsphinx.binUrl=https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64"

    如果在下载 https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64 时有问题，可以先下载到本地，然后用 `file:/tmp/sphinx.osx-x86_64` 的方式指定

* 到 https://oss.sonatype.org/ 上，“Staging Repositories”然后close掉自己的，再release
* 发布完maven仓库之后，需要到阿里云的仓库里检查是否同步，有可能有延时
* 需要更新 gh-pages 分支下面的 arthas-boot.jar/arthas-demo.jar/as.sh
* 需要更新docker镜像，push新的tag

    以 3.1.0 版本为例：
    ```
    docker build . --build-arg ARTHAS_VERSION=3.1.0 -t hengyunabc/arthas:3.1.0
    docker tag hengyunabc/arthas:3.1.0  hengyunabc/arthas:latest
    docker push hengyunabc/arthas:3.1.0
    docker push hengyunabc/arthas:latest

    docker build .  --build-arg ARTHAS_VERSION=3.1.0 -f Dockerfile-No-Jdk -t hengyunabc/arthas:3.1.0-no-jdk
    docker push hengyunabc/arthas:3.1.0-no-jdk
    ```

