


## issue

欢迎在issue里对arthas做反馈，分享使用技巧，排查问题的经历。

* https://github.com/alibaba/arthas/issues

## 改进用户文档

用户文档在`site/src/site/sphinx`目录下，如果希望改进arthas用户文档，可以提交PR。

## 开发者相关


### 安装到本地

本地开发时，可以执行`as-package.sh`来打包，会自动安装最新版本的arthas到`~/.arthas`目录里。debug时会自动使用最新版本。

也可以直接 `mvn clean package -DskipTests`打包，生成的zip在 `packaging/target/` 下面

### 全量打包

* arthas是用sphinx来生成静态网站
* 在site/pom.xml里配置了`sphinx-maven-plugin`
* `sphinx-maven-plugin`通过下载`sphinx-binary/`来执行
* sphinx配置的`recommonmark`插件有bug：https://github.com/rtfd/recommonmark/issues/93 ，因此另外打包了一个修复版本： https://github.com/hengyunabc/sphinx-binary/releases/tag/v0.4.0.1
* 全量打包时，需要配置下面的参数（目前只支持mac/linux）：

    ```
    mvn clean package -DskipTests -P full -Dsphinx.binUrl=https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64
    ```



