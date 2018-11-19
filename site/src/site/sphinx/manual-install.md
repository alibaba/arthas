手动安装Arthas
===

1. 下载最新版本

    **最新版本，点击下载**：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=bin&v=LATEST)


    如果下载速度比较慢，可以尝试用[阿里云的镜像仓库](https://maven.aliyun.com/)，比如要下载`3.x.x`版本（替换`3.x.x`为最新版本），下载的url是：

    https://maven.aliyun.com/repository/public/com/taobao/arthas/arthas-packaging/3.x.x/arthas-packaging-3.x.x-bin.zip



2. 解压缩arthas的压缩包
    ```
    unzip arthas-packaging-bin.zip
    ```

3. 安装Arthas

    安装之前最好把所有老版本的Arthas全都删掉
    ```
    sudo su admin
    rm -rf /home/admin/.arthas/lib/*
    cd arthas
    ./install-local.sh
    ```
    > 注意，这里根据你需要诊断的Java进程的所属用户进行切换

4. 启动Arthas

    启动之前，请确保老版本的Arthas已经`shutdown`.

    ```
    ./as.sh
    ```