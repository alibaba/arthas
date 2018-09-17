手动安装Arthas
===

1. 下载最新版本

    最新版本：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22)

    在`Download`栏下载最新的 `bin.zip` 包。


2. 解压缩arthas的压缩包
    ```
    unzip arthas-packaging-bin.zip
    ```

3. 安装Arthas: 安装之前最好把所有老版本的Arthas全都删掉
    ```
    sudo su admin
    rm -rf /home/admin/.arthas/lib/*
    cd arthas
    ./install-local.sh
    ```
    > 注意，这里根据你需要诊断的Java进程的所属用户进行切换

4. 启动Arthas: 启动之前，请确保老版本的Arthas已经shutdown.

    ```
    ./as.sh
    ```