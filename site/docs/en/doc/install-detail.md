# Install Arthas

## Quick installation

### Use `arthas-boot`(Recommended)

Download`arthas-boot.jar`，Start with `java` command:

```bash
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar
```

Print usage:

```bash
java -jar arthas-boot.jar -h
```

### Use `as.sh`

You can install Arthas with one single line command on Linux, Unix, and Mac. Pls. copy the following command and paste it into the command line, then press _Enter_ to run:

```bash
curl -L https://arthas.aliyun.com/install.sh | sh
```

The command above will download the bootstrap script `as.sh` to the current directory. You can move it to any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as.sh`, or execute `as.sh -h` for more help information.

`as.sh` searches for a usable Java installation in the following order and stops at the first valid candidate:

1. The `JAVA_HOME` environment variable.
2. `/opt/taobao/java`, if the directory exists.
3. The directory returned by `/usr/libexec/java_home` on macOS.
4. The `java` command on `PATH`, resolving symbolic links to locate its installation directory.
5. The installation directories of running Java processes, when the process list contains their full `/bin/java` paths.

Each candidate must contain an executable `bin/java`, and `java -version` must succeed and report a recognizable version. Java 8 and earlier also require `lib/tools.jar`. If `JAVA_HOME` points to a JRE subdirectory, the script searches up to two parent directories for a JDK containing both `tools.jar` and an executable `bin/java`. Java 9 and later do not require `tools.jar`.

A valid `JAVA_HOME` always takes precedence. If a configured installation is invalid, the script reports the reason and continues searching. If no candidate is usable, it exits before downloading or attaching. To select Java for a single invocation:

```bash
JAVA_HOME="/path/to/jdk" ./as.sh
```

## Full installation

Latest Version, Click To Download: [![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/latest_version)

Download and unzip, find `arthas-boot.jar` in the directory. Start with `java` command:

```bash
java -jar arthas-boot.jar
```

Print usage:

```bash
java -jar arthas-boot.jar -h
```

## Manual Installation

[Manual Installation](manual-install.md)

## Installation via Packages

Arthas has packages for Debian and Fedora based systems.
you can get them from the github releases page https://github.com/alibaba/arthas/releases.

### Instruction for Debian based systems

```bash
sudo dpkg -i arthas*.deb
```

### Instruction for Fedora based systems

```bash
sudo rpm -i arthas*.rpm
```

### Usage

After the installation of packages, execute

```bash
as.sh
```

## Offline Help Documentation

Latest Version Documentation, Click To Download:[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/doc/latest_version)

## Uninstall

- On Linux/Unix/Mac, delete the files with the following command:

  ```bash
  rm -rf ~/.arthas/
  rm -rf ~/logs/arthas/
  ```

- On Windows, delete `.arthas` and `logs/arthas` directory under user home.

---

::: warning
If you need to diagnose applications running on JDK 6/7, please click [here to install arthas 3](https://arthas.aliyun.com/3.x/en/doc/install-detail.html).
:::
