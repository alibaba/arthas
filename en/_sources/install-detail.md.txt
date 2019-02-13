Install Arthas
==============

## Quick installation


### Use `arthas-boot`(Recommend)

Download`arthas-boot.jar`，Start with `java` command:

```bash
wget https://alibaba.github.io/arthas/arthas-boot.jar
java -jar arthas-boot.jar
```

Print usage:

```bash
java -jar arthas-boot.jar -h
```


### Use `as.sh`

You can install Arthas with one single line command on Linux, Unix, and Mac. Pls. copy the following command and paste it into the command line, then press *Enter* to run:

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

The command above will download the bootstrap script `as.sh` to the current directory. You can move it the any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as.sh`, or execute `as.sh -h` for more help information.


## Full installation

Latest Version, Click To Download: [![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=doc&v=LATEST)

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


## Offline Help Documentation

Latest Version, Click To Download:[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://github.com/alibaba/arthas/releases/download/arthas-all-3.0.5/arthas-3.0.5-doc.zip)


## Uninstall

* On Linux/Unix/Mac, delete the files with the following command:

    ```bash
    rm -rf ~/.arthas/
    rm -rf ~/logs/arthas/
    ```

* On Windows, delete `.arthas` and `logs/arthas` directory under user home.
