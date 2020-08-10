Install Arthas
==============

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

You can install Arthas with one single line command on Linux, Unix, and Mac. Pls. copy the following command and paste it into the command line, then press *Enter* to run:

```bash
curl -L https://arthas.aliyun.com/install.sh | sh
```

The command above will download the bootstrap script `as.sh` to the current directory. You can move it to any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as.sh`, or execute `as.sh -h` for more help information.


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

* On Linux/Unix/Mac, delete the files with the following command:

    ```bash
    rm -rf ~/.arthas/
    rm -rf ~/logs/arthas/
    ```

* On Windows, delete `.arthas` and `logs/arthas` directory under user home.
