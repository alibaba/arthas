Docker
===

## Quick start with Docker

1. Delete the existing `arthas-demo` docker container (not necessary)

    ```sh
    $ docker stop arthas-demo || true && docker rm arthas-demo || true
    ```

1. Start `arthas-demo`

    ```sh
    $ docker run --name arthas-demo -it hengyunabc/arthas:latest /bin/sh -c "java -jar /opt/arthas/arthas-demo.jar"
    ```

1. Start `arthas-boot` for diagnosis

    ```sh
    $ docker exec -it arthas-demo /bin/sh -c "java -jar /opt/arthas/arthas-boot.jar"
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


    wiki: https://alibaba.github.io/arthas
    version: 3.0.5
    pid: 9
    time: 2018-12-18 11:30:36
    ```

## Diagnose the Java process in Docker

```sh
docker exec -it  ${containerId} /bin/bash -c "wget https://alibaba.github.io/arthas/arthas-boot.jar && java -jar arthas-boot.jar"
```

## Diagnose the Java process in the container in k8s

```sh
kubectl exec -it ${pod} --container ${containerId} -- /bin/bash -c "wget https://alibaba.github.io/arthas/arthas-boot.jar && java -jar arthas-boot.jar"
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