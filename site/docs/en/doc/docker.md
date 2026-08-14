# Docker

## Use JDK in Docker

Many times, the problem that arthas can't work with the application in docker is because the docker does not install JDK, but installs JRE. If only JRE is installed, many JAVA command line tools and class libraries will be missing, and Arthas will not work properly. Here are two common ways to use JDK in Docker.

### Use public JDK image

- https://hub.docker.com/_/openjdk/

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

## Diagnose a Java process in Kubernetes

If the application container already includes a shell, a full JDK, and a download tool, start Arthas directly inside the application container.

```sh
kubectl exec -it ${pod} --container ${containerName} -- /bin/bash -c "wget https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar"
```

### Attach from an ephemeral debug container

Starting with [4.3.4](https://github.com/alibaba/arthas/commit/21cf2e9ba52b305290be7223b980ff504bb9cb5b), Arthas can attach to a JVM in an application container from an ephemeral container created by `kubectl debug`. This is useful when the application uses a distroless image or does not include a shell or JDK.

Make sure the following requirements are met.

- The Pod runs on Linux.
- The Kubernetes cluster supports ephemeral containers, and the container runtime supports `kubectl debug --target`. The target JVM's PID must be visible from the debug container.
- The debug container provides a full JDK and uses Arthas 4.3.4 or later.
- If the debug container and target JVM use different UIDs, the debug container must run as root and be able to access `/proc/<pid>/root` for the target process. The target HotSpot must also accept an attach client with UID `0`. Some older JDKs accept only the same UID/GID; those versions require matching IDs in both containers.

:::: warning
The `--profile=sysadmin` option below creates a privileged ephemeral container. Use it only when authorized and permitted by the cluster security policy. If the application Pod enforces a non-root user, ask the cluster administrator for a debug configuration with the required permissions.
::::

Add an ephemeral container to the running Pod. `${containerName}` is the name of the target application container.

```sh
kubectl debug -it ${pod} \
  --target=${containerName} \
  --image=amazoncorretto:8-alpine-jdk \
  --profile=sysadmin \
  -- sh
```

The `--target` option places the ephemeral container in the target container's PID namespace. See [Debug Running Pods](https://kubernetes.io/docs/tasks/debug/debug-application/debug-running-pod/) for configuration details that vary between Kubernetes versions.

Inside the debug container, download and extract the complete Arthas 4.3.4 distribution.

```sh
ARTHAS_VERSION=4.3.4
ARTHAS_HOME=/tmp/arthas-${ARTHAS_VERSION}

wget -qO /tmp/arthas.zip \
  "https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/${ARTHAS_VERSION}/arthas-packaging-${ARTHAS_VERSION}-bin.zip"
mkdir -p "${ARTHAS_HOME}"
unzip -q /tmp/arthas.zip -d "${ARTHAS_HOME}"
```

Use `ps` to find the target JVM's PID. The following example uses PID `1`.

```sh
ps -ef

TARGET_PID=1
java -jar "${ARTHAS_HOME}/arthas-boot.jar" "${TARGET_PID}"
```

Arthas automatically compares the mount namespaces of the debug container and the target JVM. When they differ, Arthas exposes the target JVM's attach socket and copies the complete Arthas Home into a temporary directory in the target container. No shared volume is required between the two containers.

After a successful cross-mount-namespace attach, the target JVM's `arthas.log` contains the following structured event.

```text
event=arthas_attach status=success mode=cross-mount-namespace targetPid=... arthasHome=... network=... telnet=... http=...
```

Run `stop` to stop Arthas and remove the temporary Arthas Home copied into the target container. The `quit` and `exit` commands only disconnect the current session; they do not stop Arthas or remove that directory.

If `ps` in the debug container does not show the target JVM, the container runtime did not share the target process namespace as requested by `--target`, so this attach method cannot work. If the post-attach server health check fails, the terminal directs you to the target JVM's `arthas.log`. You can locate the log from the debug container.

```sh
find /proc/${TARGET_PID}/root -name arthas.log 2>/dev/null
```

### Attach from a sidecar container

You can also preinstall a full JDK and Arthas in a long-running sidecar, then attach from the sidecar to an application JVM in the same Pod. Set [`shareProcessNamespace: true`](https://kubernetes.io/docs/tasks/configure-pod-container/share-process-namespace/) when the Pod is created. Otherwise, the sidecar cannot see the application container's PID. This setting shares only the PID namespace. The containers still have separate mount namespaces, and Arthas 4.3.4 accesses the target filesystem through `/proc/<pid>/root`, so no shared volume is required. Containers in a Pod already share a network namespace, so `NET_ADMIN` is not required either.

Prefer running the sidecar and application JVM with the same Linux UID and GID. With a normally attachable JVM, the default `/proc` security policy, and a writable temporary directory, this path does not require any additional Linux capabilities.

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

If the sidecar and application JVM use different UIDs, the sidecar must run as UID `0`, and the target HotSpot must accept a root client. The current [OpenJDK attach listener](https://github.com/openjdk/jdk/blob/master/src/hotspot/os/posix/attachListener_posix.cpp#L253-L269) accepts either the same UID/GID or UID `0`. The upstream [OpenJDK 8u implementation](https://github.com/openjdk/jdk8u/blob/master/hotspot/src/os/linux/vm/attachListener_linux.cpp#L350-L358) still requires an exact UID/GID match, and vendor builds may have different policies. This check runs inside the target JVM and cannot be bypassed by adding Linux capabilities. Use the same-UID/GID configuration for targets with this behavior.

When the target HotSpot accepts a root client, explicitly retain the following four capabilities to support the initial attach, temporary directories with restrictive permissions, and cleanup by `stop`.

| Setting or capability         | Purpose                                                                                                     |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `shareProcessNamespace: true` | Makes the application JVM's PID and `/proc/<pid>` visible to the sidecar                                    |
| `runAsUser: 0`                | Passes the cross-UID check when the target HotSpot accepts root clients                                     |
| `SYS_PTRACE`                  | Passes ptrace access checks for `/proc/<pid>`, `/proc/<pid>/root`, and mount namespace information          |
| `KILL`                        | Sends `SIGQUIT` across UIDs when the target JVM's attach listener has not started                           |
| `DAC_OVERRIDE`                | Connects to the target user's `0600` attach socket and writes to the target container's temporary directory |
| `CHOWN`                       | Gives the copied Arthas Home to the target user so that `stop` can remove it                                |

The following `securityContext` fragment is for a cross-UID sidecar. It does not require `privileged: true`, `SYS_ADMIN`, `hostPID`, or `hostPath`.

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

Container runtime default capability sets usually include `KILL`, `DAC_OVERRIDE`, and `CHOWN`, so adding only `SYS_PTRACE` is often sufficient when the defaults are retained. The example above drops `ALL` and adds each required capability explicitly, making the result independent of runtime defaults.

:::: warning
The Kubernetes [Baseline and Restricted Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/) both disallow adding `SYS_PTRACE`; Restricted also disallows UID `0`. A cluster administrator must provide an exemption or a dedicated namespace for this trusted cross-UID workload. A shared PID namespace also exposes process arguments, environment variables, and `/proc/<pid>/root` for other containers in the Pod. Do not make this sidecar available to untrusted users.
::::

The Arthas attach process does not call the Kubernetes API, so the sidecar ServiceAccount needs no additional RBAC permissions. An operator who enters the sidecar with `kubectl exec` still needs the corresponding `pods/exec` permission.

The sidecar image must contain a full JDK and Arthas 4.3.4 or later. With a shared PID namespace, the application process is usually no longer PID `1`; use `ps` to find its actual PID first.

```sh
kubectl exec -it ${pod} --container arthas-sidecar -- sh

ps -ef
TARGET_PID=<java-pid>
java -jar /opt/arthas/arthas-boot.jar "${TARGET_PID}"
```

If the same-UID/GID configuration still cannot attach, check whether the target JVM uses `-XX:+DisableAttachMechanism`, and whether seccomp, AppArmor, SELinux, or the `/proc` mount policy blocks process access. If the target JVM uses a custom `java.io.tmpdir`, that directory must also be writable by the attach client.

## Install Arthas into the base Docker image

It's easy to install Arthas into your Docker image.

```
FROM openjdk:8-jdk-alpine

# copy arthas
COPY --from=hengyunabc/arthas:latest /opt/arthas /opt/arthas
```

If you want to specify a version, you can view all the tags:

[https://hub.docker.com/r/hengyunabc/arthas/tags](https://hub.docker.com/r/hengyunabc/arthas/tags)
