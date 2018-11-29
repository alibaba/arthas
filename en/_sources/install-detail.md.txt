Install Arthas
==============

## All Platform (recommended)

Latest Version, Click To Download: [![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=bin&v=LATEST)

Download and unzip, find `arthas-boot.jar` in the directory. Start with `java` command:

```bash
java -jar arthas-boot.jar
```

Print help information:

```bash
java -jar arthas-boot.jar -h
```


## Linux/Unix/Mac

You can install Arthas with one single line command on Linux, Unix, and Mac. Pls. copy the following command and paste it into the command line, then press *Enter* to run:

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

The command above will download the bootstrap script `as.sh` to the current directory. You can move it the any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as.sh`, or execute `as.sh -h` for more help information.

## Windows

Latest Version, Click To Download: [![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=bin&v=LATEST)

Download and unzip, then find `as.bat` from 'bin' directory. For now this script will only take one argument `pid`, which means you can only diagnose the local Java process. (Welcome any bat script expert to make it better :heart:)

```bash
as.bat <pid>
```

## Manual Installation

[Manual Installation](manual-install.md)

If you fail to boot Arthas with the provided batch file, you could try to assemble the bootstrap command in the following way.


1. Locate java in the target JVM:

    - Linux/Unix/Mac: `ps aux | grep java`
    - Windows: open the Process Monitor to search java

2. Assemble bootstrap command:
    
    Let's suppose we are using `/opt/jdk1.8/bin/java`, then the command should be:

    ```bash
    /opt/jdk1.8/bin/java -Xbootclasspath/a:/opt/jdk1.8/lib/tools.jar \
        -jar /tmp/arthas-packaging/arthas-core.jar \
        -pid 15146 \
        -target-ip 127.0.0.1 -telnet-port 3658 -http-port 8563 \
        -core /tmp/arthas-packaging/arthas-core.jar \
        -agent /tmp/arthas-packaging/arthas/arthas-agent.jar
    ```

    Note:
    
    * `-Xbootclasspath` adds tools.jar
    * `-jar /tmp/arthas-packaging/arthas-core.jar` specifies main entry
    * `-pid 15146` specifies the target java process PID
    * `-target-ip 127.0.0.1` specifies the IP
    * `-telnet-port 3658 -http-port 8563` specifies telnet and HTTP ports for remote access
    * `-core /tmp/arthas-packaging/arthas-core.jar -agent /tmp/arthas-packaging/arthas/arthas-agent.jar` specifies core/agent jar package

    If you are running on JDK 1.9 or above，then it's unncessary to add `tools.jar` in option `-Xbootclasspath`.
    
    You can find the logs from `~/logs/arthas/arthas.log`.

3. Use telnet to connect once attaching to the target JVM (in step 2) succeeds

    ```bash
    telnet 127.0.0.1 3658
    ```

## Offline Help Documentation

Latest Version, Click To Download:[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=doc&v=LATEST)


## Uninstall

* On Linux/Unix/Mac, delete the files with the following command:

    ```bash
    rm -rf ~/.arthas/
    ```

* On Windows, simply delete the zip file and unzipped files. 
