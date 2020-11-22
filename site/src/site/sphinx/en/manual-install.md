Manually Install Arthas
===================

## Manually Install Arthas

1. Download the latest version

    **Latest version, Click To Download**: [![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/latest_version)


2. Unzip zip file

    ```bash
    unzip arthas-packaging-bin.zip
    ```

3. Install Arthas

    It is recommended to completely remove all old versions of Arthas before installation.

    ```bash
    sudo su admin
    rm -rf /home/admin/.arthas/lib/* # remove all the leftover of the old outdated Arthas
    cd arthas
    ./install-local.sh # switch the user based on the owner of the target Java process.
    ```

4. Start Arthas

    Make sure `stop` the old Arthas server before start.

    ```bash
    ./as.sh
    ```


## Startup with as.sh/as.bat

### Linux/Unix/Mac

You can install Arthas with one single line command on Linux, Unix, and Mac. Pls. copy the following command and paste it into the command line, then press *Enter* to run:

```bash
curl -L https://arthas.aliyun.com/install.sh | sh
```

The command above will download the bootstrap script `as.sh` to the current directory. You can move it the any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as.sh`, or execute `as.sh -h` for more help information.

### Windows

Latest Version, Click To Download: [![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/latest_version)

Download and unzip, then find `as.bat` from 'bin' directory. For now this script will only take one argument `pid`, which means you can only diagnose the local Java process. (Welcome any bat script expert to make it better :heart:)

```bash
as.bat <pid>
```

If you want to diagnose Java process run as windows service, try these commands:

```bash
as-service.bat -port <port>
as-service.bat -pid <pid>
as-service.bat -pid <pid> --interact
```

Use this command to remove arthas service: 
```bash
as-service.bat -remove
```

## Manual command line startup

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

    If you are running on JDK 1.9 or above，then it's unnecessary to add `tools.jar` in option `-Xbootclasspath`.
    
    You can find the logs from `~/logs/arthas/arthas.log`.

3. Use telnet to connect once attaching to the target JVM (in step 2) succeeds

    ```bash
    telnet 127.0.0.1 3658
    ```
