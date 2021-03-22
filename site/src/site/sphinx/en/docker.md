Docker
===


## Use JDK in Docker

Many times, the problem that arthas can't work with the application in docker is because the docker does not install JDK, but installs JRE. If only JRE is installed, many JAVA command line tools and class libraries will be missing, and Arthas will not work properly. Here are two common ways to use JDK in Docker.

### Use public JDK image

* https://hub.docker.com/_/openjdk/

such as:

```
FROM openjdk:8-jdk
```

or:

```
FROM openjdk:8-jdk-alpine
```

### Install via package management software

such as:

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

# Setup JAVA_HOME - useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME
```

or:

```bash
RUN yum install -y \
   java-1.8.0-openjdk \
   java-1.8.0-openjdk-devel

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk/
RUN export JAVA_HOME
```


## Quick start with Docker

1. Delete the existing `math-game` docker container (not necessary)

    ```sh
    $ docker stop math-game || true && docker rm math-game || true
    ```

1. Start `math-game`

    ```sh
    $ docker run --name math-game -it hengyunabc/arthas:latest /bin/sh -c "java -jar /opt/arthas/math-game.jar"
    ```

1. Start `arthas-boot` for diagnosis

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

## Diagnose the Java process in Docker

```sh
docker exec -it  ${containerId} /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

## Diagnose the Java process in the container in k8s

```sh
kubectl exec -it ${pod} --container ${containerId} -- /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

## Install Arthas into the base Docker image

It's easy to install Arthas into your Docker image.

```
FROM openjdk:8-jdk-alpine

# copy arthas
COPY --from=hengyunabc/arthas:latest /opt/arthas /opt/arthas
```

If you want to specify a version, you can view all the tags:

[https://hub.docker.com/r/hengyunabc/arthas/tags](https://hub.docker.com/r/hengyunabc/arthas/tags)