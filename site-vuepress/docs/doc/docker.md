Docker
===

## 在Docker里使用JDK

很多时候，应用在docker里出现arthas无法工作的问题，是因为应用没有安装 JDK ，而是安装了 JRE 。如果只安装了 JRE，则会缺少很多JAVA的命令行工具和类库，Arthas也没办法正常工作。下面介绍两种常见的在Docker里使用JDK的方式。

### 使用公开的JDK镜像

* https://hub.docker.com/_/openjdk/

比如：

```
FROM openjdk:8-jdk
```

或者：

```
FROM openjdk:8-jdk-alpine
```

### 通过包管理软件来安装

比如：

```bash
# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME
```

或者：

```bash
RUN yum install -y \
   java-1.8.0-openjdk \
   java-1.8.0-openjdk-devel

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk/
RUN export JAVA_HOME
```

## 通过Docker快速入门

1. 删除本地已有的`math-game` docker container（非必要）

    ```sh
    $ docker stop math-game || true && docker rm math-game || true
    ```

1. 启动`math-game`

    ```sh
    $ docker run --name math-game -it hengyunabc/arthas:latest /bin/sh -c "java -jar /opt/arthas/math-game.jar"
    ```

1. 启动`arthas-boot`来进行诊断

    ```sh
    $ docker exec -it math-game /bin/sh -c "java -jar /opt/arthas/arthas-boot.jar"
    * [1]: 9 jar

    [INFO] arthas home: /opt/arthas
    [INFO] Try to attach process 9
    [INFO] Attach process 9 success.
    [INFO] arthas-client connect 127.0.0.1 3658
    ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
    /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
    |  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
    |  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
    `--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


    wiki: https://arthas.aliyun.com/doc
    version: 3.0.5
    pid: 9
    time: 2018-12-18 11:30:36
    ```

## 诊断Docker里的Java进程

```sh
docker exec -it  ${containerId} /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

## 诊断k8s里容器里的Java进程

```sh
kubectl exec -it ${pod} --container ${containerId} -- /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

## 把Arthas安装到基础镜像里

可以很简单把Arthas安装到你的Docker镜像里。

```
FROM openjdk:8-jdk-alpine

# copy arthas
COPY --from=hengyunabc/arthas:latest /opt/arthas /opt/arthas
```

如果想指定版本，可以查看具体的tags：

[https://hub.docker.com/r/hengyunabc/arthas/tags](https://hub.docker.com/r/hengyunabc/arthas/tags)