


## issue

欢迎在issue里对arthas做反馈，分享使用技巧，排查问题的经历。

* https://github.com/alibaba/arthas/issues

## 改进用户文档

用户文档在`site/src/site/sphinx`目录下，如果希望改进arthas用户文档，可以提交PR。

## 开发者相关


### 安装到本地

本地开发时，推荐执行`as-package.sh`来打包，会自动安装最新版本的arthas到`~/.arthas`目录里。debug时会自动使用最新版本。

`as.sh`在启动时，会对`~/.arthas/lib`下面的目录排序，取最新的版本。`as-package.sh`在打包时，会取`pom.xml`里的版本号，再拼接上当前时间，比如： `3.0.5.20180917161808`，这样子排序时取的就是最新的版本。

也可以直接 `./mvnw clean package -DskipTests`打包，生成的zip在 `packaging/target/` 下面。但是注意`as.sh`启动加载的是`~/.arthas/lib`下面的版本。

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



