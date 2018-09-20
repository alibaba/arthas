Arthas Install
==============

## Linux/Unix/Mac

You can install Arthas in one single line as:

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

The command line above will download the booting script `as.sh` to the current directory and you can then start Arthas by `./as.sh`, for more help info you can use `./as.sh -h` to check the details. 

By the way, you can also add the absolute path of the script `as.sh` to `$PATH` to make it available globally. 

## Windows

Latest Version: [![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22)

Download the latest `bin.zip`, unzip the package and you can find the `as.bat`. For now this script will only take one argument `pid` which means you can only diagnose the local Java process. (Welcome any bat script expert to make it better :heart:)

```bash
as.bat <pid>
```

A small tip: 

If you are asking for better UX, you can start the Arthas Server locally by `as.bat <pid>` and then use `./as.sh <pid>@<ip>:<por>`in another Linux/Unix/Mac machine. 


Another tip:

If in Windows, the color is not working as expect. You can try [conemu](https://sourceforge.net/projects/conemu) to get it to work. 

## Manual Installation

[Manual Installation](manual-install.md)

## Advanced Manual Boot

If you cannot boot Arthas, try to pass in all the critical options manually as the following steps:

1. locate the java for JVM:

    - Linux/Unix/Mac: `ps aux | grep java`
    - Windows: open the Process Monitor to search java

2. Concatenate the command
    
    Let's suppose we are using `/opt/jdk1.8/bin/java`, then the command should be:

    ```bash
    /opt/jdk1.8/bin/java -Xbootclasspath/a:/opt/jdk1.8/lib/tools.jar \
        -jar /tmp/arthas-packaging/arthas-core.jar \
        -pid 15146 \
        -target-ip 127.0.0.1 -telnet-port 3658 -http-port 8563 \
        -core /tmp/arthas-packaging/arthas-core.jar \
        -agent /tmp/arthas-packaging/arthas/arthas-agent.jar
    ```

    * `-Xbootclasspath` add tools.jar
    * `-jar /tmp/arthas-packaging/arthas-core.jar` specify main entry
    * `-pid 15146` specify the target java process pid
    * `-target-ip 127.0.0.1` specify the IP
    * `-telnet-port 3658 -http-port 8563` specify telnet and http ports
    * `-core /tmp/arthas-packaging/arthas-core.jar -agent /tmp/arthas-packaging/arthas/arthas-agent.jar` specify core/agent jar package


    But if you are using JDK 1.9 or above，then you do not need to add `tools.jar` in option `-Xbootclasspath`.

    F.Y.I the booting log will be printed to `~/logs/arthas/arthas.log`.

3. Connect via telnet

    When attached successfully, you can connect it with 

    ```bash
    telnet localhost 3658
    ```

## Offline Help Documentation

Latest Version:[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taoba)

## Uninstall

### Linux/Unix/Mac

```bash
rm -rf ~/.arthas/ ~/.arthas_history
```

### Windows

Directly delete the `zip` and unzipped files. 
