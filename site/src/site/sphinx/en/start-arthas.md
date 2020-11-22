Start Arthas
=====

## Interactive Mode

```bash
./as.sh
```

```bash
➜  bin git:(develop) ✗ ./as.sh
Found existing java process, please choose one and input the serial number of the process, eg : 1. Then hit ENTER.
  [1]: 3088 org.jetbrains.idea.maven.server.RemoteMavenServer
* [2]: 12872 org.apache.catalina.startup.Bootstrap
  [3]: 2455
Attaching to 12872...
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'
$
```

## Non-Interactive Mode

Startup script is as follows:

```bash
./as.sh <PID>[@IP:PORT]
```

### Parameter Description

* *PID*: Target Java process ID (Make sure that the user executing the command has sufficient permissions to operate the target Java process.)
* *IP*: The address that Arthas Server listens on, the default value is `127.0.0.1`. Arthas allows multiple users to access simultaneously without interfering with each other.
* *PORT*: Arthas Server port，the default value is 3658

### Sample

* If IP and PORT are not specified, then the default values are 127.0.0.1 and 3658

	> ./as.sh 12345

	Equivalent to:
	
	> ./as.sh 12356@127.0.0.1:3658

### Remote Diagnosis

After starting Arthas Server on the target Java process, users can use `telnet` connect to the remote Arthas Server, for example：

```bash
telnet 192.168.1.119 3658
```
	
### sudo Support

Usually online environment will only grant users privilege as low as possible, instead, all advanced operations are through sudo-list. Since `as.sh` script takes into account the current effective user, it is possible to run the script in the other rule, by specifying `-H` option like this:

```bash
sudo -u admin -H ./as.sh 12345
```


### Windows Support

Right now `as.bat` script supports one parameter only, which is: pid

```bash
as.bat <pid>
```
