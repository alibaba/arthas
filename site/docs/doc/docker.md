# Docker

## 在 Docker 里使用 JDK

很多时候，应用在 docker 里出现 arthas 无法工作的问题，是因为应用没有安装 JDK ，而是安装了 JRE 。如果只安装了 JRE，则会缺少很多 JAVA 的命令行工具和类库，Arthas 也没办法正常工作。下面介绍两种常见的在 Docker 里使用 JDK 的方式。

### 使用公开的 JDK 镜像

- https://hub.docker.com/_/openjdk/

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

## 通过 Docker 快速入门

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

## 诊断 Docker 里的 Java 进程

```sh
docker exec -it  ${containerId} /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

## 诊断 k8s 里容器里的 Java 进程

如果应用容器已经包含 shell、完整 JDK 和下载工具，可以直接进入应用容器启动 Arthas。

```sh
kubectl exec -it ${pod} --container ${containerName} -- /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

### 从临时 debug 容器 attach

从 [4.3.4](https://github.com/alibaba/arthas/commit/21cf2e9ba52b305290be7223b980ff504bb9cb5b) 开始，Arthas 可以从 `kubectl debug` 创建的临时容器 attach 应用容器里的 JVM。应用镜像使用 distroless，或者没有 shell、JDK 时，可以用这个方式把诊断工具临时加入正在运行的 Pod。

使用前需要满足下面的条件。

- Pod 运行在 Linux 上。
- Kubernetes 集群支持临时容器，容器运行时支持 `kubectl debug --target`。进入临时容器后必须能看到目标 JVM 的 PID。
- 临时容器提供完整 JDK，并使用 Arthas 4.3.4 或更高版本。
- 临时容器与目标 JVM 的 UID 不同时，临时容器需要以 root 身份运行，并能访问目标进程的 `/proc/<pid>/root`。目标 HotSpot 还必须允许 UID `0` 的 attach 客户端。部分旧 JDK 只接受相同 UID/GID，此时需要让两个容器使用相同 UID/GID。

:::: warning
下面的 `--profile=sysadmin` 会创建 privileged 临时容器。请只在获得授权且集群安全策略允许时使用。应用 Pod 强制以非 root 用户运行时，需要由集群管理员提供具备相应权限的调试配置。
::::

先把临时容器加入正在运行的 Pod。`${containerName}` 是目标应用容器的名称。

```sh
kubectl debug -it ${pod} \
  --target=${containerName} \
  --image=amazoncorretto:8-alpine-jdk \
  --profile=sysadmin \
  -- sh
```

`--target` 让临时容器进入目标容器的 PID namespace。不同 Kubernetes 版本的配置方式可以参考 [Kubernetes 调试运行中的 Pod](https://kubernetes.io/zh-cn/docs/tasks/debug/debug-application/debug-running-pod/)。

进入临时容器后，下载并解压完整的 Arthas 4.3.4 发行包。

```sh
ARTHAS_VERSION=4.3.4
ARTHAS_HOME=/tmp/arthas-${ARTHAS_VERSION}

wget -qO /tmp/arthas.zip \
  "https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/${ARTHAS_VERSION}/arthas-packaging-${ARTHAS_VERSION}-bin.zip"
mkdir -p "${ARTHAS_HOME}"
unzip -q /tmp/arthas.zip -d "${ARTHAS_HOME}"
```

用 `ps` 查找目标 JVM 的 PID。下面以 PID `1` 为例。

```sh
ps -ef

TARGET_PID=1
java -jar "${ARTHAS_HOME}/arthas-boot.jar" "${TARGET_PID}"
```

Arthas 会自动比较临时容器和目标 JVM 的 mount namespace。检测到两者不同时，Arthas 会暴露目标 JVM 的 attach socket，并把完整的 Arthas Home 复制到目标容器的临时目录。整个过程不需要两个容器挂载共享目录。

跨 mount namespace attach 成功后，目标 JVM 的 `arthas.log` 会记录下面的结构化日志。

```text
event=arthas_attach status=success mode=cross-mount-namespace targetPid=... arthasHome=... network=... telnet=... http=...
```

执行 `stop` 会停止 Arthas，并清理复制到目标容器里的临时 Arthas Home。`quit` 和 `exit` 只断开当前连接，不会停止 Arthas 或清理该目录。

如果临时容器中的 `ps` 看不到目标 JVM，说明容器运行时没有按 `--target` 共享目标进程，当前方式无法 attach。如果 attach 后的服务启动检查失败，终端会提示查看目标 JVM 的 `arthas.log`，可以从临时容器中查找日志。

```sh
find /proc/${TARGET_PID}/root -name arthas.log 2>/dev/null
```

### 从 sidecar 容器 attach

也可以把完整 JDK 和 Arthas 预装在常驻 sidecar 容器中，再从 sidecar attach 同一个 Pod 里的应用 JVM。创建 Pod 时必须设置 [`shareProcessNamespace: true`](https://kubernetes.io/zh-cn/docs/tasks/configure-pod-container/share-process-namespace/)，否则 sidecar 看不到应用容器的 PID。这个字段只共享 PID namespace。两个容器仍使用独立的 mount namespace，Arthas 4.3.4 会通过 `/proc/<pid>/root` 处理文件访问，不需要共享 volume。Pod 内的容器已经共享 network namespace，因此也不需要 `NET_ADMIN`。

优先让 sidecar 和应用 JVM 使用相同的 Linux UID 和 GID。普通 JVM、默认 `/proc` 安全策略和可写临时目录下，这条路径不需要增加 Linux capability。

```yaml
spec:
  shareProcessNamespace: true
  containers:
    - name: app
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
    - name: arthas-sidecar
      image: your-registry/arthas-sidecar:4.3.4
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
        runAsNonRoot: true
        allowPrivilegeEscalation: false
        capabilities:
          drop: ["ALL"]
```

如果 sidecar 和应用 JVM 的 UID 不同，sidecar 必须以 UID `0` 运行，并且目标 HotSpot 必须允许 root 客户端。较新的 [OpenJDK attach listener](https://github.com/openjdk/jdk/blob/master/src/hotspot/os/posix/attachListener_posix.cpp#L253-L269) 接受相同 UID/GID 或 UID `0`。上游 [OpenJDK 8u 的实现](https://github.com/openjdk/jdk8u/blob/master/hotspot/src/os/linux/vm/attachListener_linux.cpp#L350-L358) 仍要求 UID/GID 完全一致，部分厂商构建也可能有不同策略。这个校验发生在目标 JVM 内部，增加 Linux capability 无法绕过。目标 JVM 使用这类实现时，只能采用相同 UID/GID 的配置。

目标 HotSpot 支持 root 客户端时，为了覆盖首次 attach、不同权限的临时目录和 `stop` 清理，建议显式保留下面四项 capability。

| 配置或 capability             | 用途                                                                                           |
| ----------------------------- | ---------------------------------------------------------------------------------------------- |
| `shareProcessNamespace: true` | 让 sidecar 能看到应用 JVM 的 PID 和 `/proc/<pid>`                                              |
| `runAsUser: 0`                | 在目标 HotSpot 支持 root 客户端时通过跨 UID 校验                                               |
| `SYS_PTRACE`                  | 通过 ptrace access check 读取 `/proc/<pid>`，并访问 `/proc/<pid>/root` 和 mount namespace 信息 |
| `KILL`                        | UID 不同时向尚未启动 attach listener 的 JVM 发送 `SIGQUIT`                                     |
| `DAC_OVERRIDE`                | 连接目标用户拥有的 `0600` attach socket，并写入目标容器的临时目录                              |
| `CHOWN`                       | 把复制到目标容器的 Arthas Home 交给目标用户，使 `stop` 可以清理该目录                          |

下面是跨 UID 场景的 `securityContext` 片段。它不需要 `privileged: true`、`SYS_ADMIN`、`hostPID` 或 `hostPath`。

```yaml
spec:
  shareProcessNamespace: true
  containers:
    - name: arthas-sidecar
      image: your-registry/arthas-sidecar:4.3.4
      securityContext:
        runAsUser: 0
        runAsGroup: 0
        allowPrivilegeEscalation: false
        seccompProfile:
          type: RuntimeDefault
        capabilities:
          drop: ["ALL"]
          add:
            - SYS_PTRACE
            - KILL
            - DAC_OVERRIDE
            - CHOWN
```

容器运行时的默认 capability 集合通常已经包含 `KILL`、`DAC_OVERRIDE` 和 `CHOWN`，所以保留默认集合时往往只需要额外添加 `SYS_PTRACE`。上面的写法先 drop `ALL` 再逐项添加，结果不依赖运行时默认值。

:::: warning
Kubernetes 的 [Baseline 和 Restricted Pod Security Standards](https://kubernetes.io/zh-cn/docs/concepts/security/pod-security-standards/) 都不允许增加 `SYS_PTRACE`，Restricted 还禁止 UID `0`。使用跨 UID 配置时，需要由集群管理员为这个受信任的工作负载提供例外或专用 namespace。共享 PID namespace 也会让 sidecar 看到同一 Pod 内其他进程的参数、环境变量和 `/proc/<pid>/root`，不要把这个 sidecar 开放给不受信任的用户。
::::

Arthas attach 本身不调用 Kubernetes API，因此 sidecar 的 ServiceAccount 不需要额外 RBAC 权限。通过 `kubectl exec` 进入 sidecar 的操作人员仍需对应的 `pods/exec` 权限。

sidecar 镜像需要包含完整 JDK 和 Arthas 4.3.4 或更高版本。启用共享 PID namespace 后，应用进程通常不再是 PID `1`，请先用 `ps` 查找实际 PID。

```sh
kubectl exec -it ${pod} --container arthas-sidecar -- sh

ps -ef
TARGET_PID=<java-pid>
java -jar /opt/arthas/arthas-boot.jar "${TARGET_PID}"
```

如果相同 UID/GID 的配置仍然无法 attach，检查目标 JVM 是否设置了 `-XX:+DisableAttachMechanism`，以及 seccomp、AppArmor、SELinux 或 `/proc` 挂载策略是否阻止了进程访问。目标 JVM 使用自定义 `java.io.tmpdir` 时，该目录也必须允许 attach 客户端写入。

## 把 Arthas 安装到基础镜像里

可以很简单把 Arthas 安装到你的 Docker 镜像里。

```
FROM openjdk:8-jdk-alpine

# copy arthas
COPY --from=hengyunabc/arthas:latest /opt/arthas /opt/arthas
```

如果想指定版本，可以查看具体的 tags：

[https://hub.docker.com/r/hengyunabc/arthas/tags](https://hub.docker.com/r/hengyunabc/arthas/tags)
