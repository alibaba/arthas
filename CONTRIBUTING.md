

## Issue

Welcome to use [issue tracker](https://github.com/alibaba/arthas/issues) to give us :bowtie::

* feedbacks - what you would like to have;
* usage tips - what usages you have found splendid;
* experiences - how you use Arthas to do **effective** troubleshooting;

## Documentation

Welcome PR to further improve English [documentation](https://github.com/alibaba/arthas/tree/master/site/src/site/sphinx/en).

## Developer

Compilation requires JDK 7 and above since we are `java.lang.management.BufferPoolMXBean` while runtime requires JDK 6.

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

There are two stages when booting Arthas:

#### Stage 1

Execute `com.taobao.arthas.core.Arthas`, locate a proper JVM and attach to it. 

If you intend to debug this part, you can: 

```bash
./as.sh debug  pid
```

The JPDA port is `8888`, you can connect to it from remote to debug; if you want to make it suspend after start, you can:

```bash
JPDA_SUSPEND=y ./as.sh debug  pid
```

#### Stage 2
After attaching, Arthas agent runs inside the target process. If you want to debug agent, you have to make sure the target process is started in debug mode too. If so, then you can directly import the Arthas source code, and debug it. 

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

编绎要求jdk7以上。因为使用到了jdk7才有的`java.lang.management.BufferPoolMXBean`，运行时要求是jdk6。

### 安装到本地

本地开发时，推荐执行`as-package.sh`来打包，会自动安装最新版本的arthas到`~/.arthas`目录里。debug时会自动使用最新版本。

`as.sh`在启动时，会对`~/.arthas/lib`下面的目录排序，取最新的版本。`as-package.sh`在打包时，会取`pom.xml`里的版本号，再拼接上当前时间，比如： `3.0.5.20180917161808`，这样子排序时取的就是最新的版本。

也可以直接 `./mvnw clean package -DskipTests`打包，生成的zip在 `packaging/target/` 下面。但是注意`as.sh`启动加载的是`~/.arthas/lib`下面的版本。

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

Arthas启动过程分为两部分：

1. 执行`com.taobao.arthas.core.Arthas`，查找到合适的jvm，然后attach上面。如果想debug这部分代码，可以直接

    ```
    ./as.sh debug  pid
    ```
    JPDA端口是`8888`，然后可以远程连接来debug。
    如果启动时等待，则
    ```
    JPDA_SUSPEND=y ./as.sh debug  pid
    ```

1. attach成功之后，arthas agent运行目标进程里。想要调试agent的代码，则需要目标进程本身是以debug方式启动的。可以直接引入arthas的source，打断点

### 全量打包

* arthas是用sphinx来生成静态网站
* 在site/pom.xml里配置了`sphinx-maven-plugin`
* `sphinx-maven-plugin`通过下载`sphinx-binary/`来执行
* sphinx配置的`recommonmark`插件有bug：https://github.com/rtfd/recommonmark/issues/93 ，因此另外打包了一个修复版本： https://github.com/hengyunabc/sphinx-binary/releases/tag/v0.4.0.1
* 全量打包时，需要配置下面的参数（目前只支持mac/linux）：

    ```
    ./mvnw clean package -DskipTests -P full -Dsphinx.binUrl=https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64
    ```



