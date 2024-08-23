# Install Arthas

## Quick installation

### Use `arthas-boot3`(Recommended)

Download`arthas-boot3.jar`，Start with `java` command:

```bash
curl -O https://arthas.aliyun.com/arthas-boot3.jar
java -jar arthas-boot3.jar
```

Print usage:

```bash
java -jar arthas-boot3.jar -h
```

### Use `as3.sh`

You can install Arthas with one single line command on Linux, Unix, and Mac. Pls. copy the following command and paste it into the command line, then press _Enter_ to run:

```bash
curl -L https://arthas.aliyun.com/install3.sh | sh
```

The command above will download the bootstrap script `as3.sh` to the current directory. You can move it to any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as3.sh`, or execute `as3.sh -h` for more help information.

## Full installation

Download from Github Releases

[https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

Download and unzip, find `arthas-boot3.jar` in the directory. Start with `java` command:

```bash
java -jar arthas-boot3.jar
```

Print usage:

```bash
java -jar arthas-boot3.jar -h
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
as3.sh
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
