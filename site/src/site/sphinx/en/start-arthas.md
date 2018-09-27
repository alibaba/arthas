Start Arthas
=====

## Interactive Mode

```bash
./as.sh
```

```bash
➜  bin git:(develop) ✗ ./as.sh
Found existing java process, please choose one and hit RETURN.
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

* PID: Target Java process ID(Make sure that the user executing the command has sufficient permissions to operate the target Java process.)

* IP: The address that Arthas Server listens on, the default value is `127.0.0.1`. Arthas allows multiple users to access simultaneously without interfering with each other.

* PORT: Arthas Server port，the default value is 3658

### Sample

* If you do not specify IP and PORT, the default is 127.0.0.1 and 3658

	> ./as.sh 12345

	Equivalent to:
	
	> ./as.sh 12356@127.0.0.1:3658

### Remote Diagnosis

After starting Arthas Server, users can use `telnet` connect to the remote Arthas Server, for example：

```bash
telnet 192.168.1.119 3658
```
	
### sudo Support

If you need to switch users, such as `admin`, you need to add the -H parameter.

```bash
sudo -u admin -H ./as.sh 12345
```


### Windows Support

`as.bat` script only supports one parameter: pid

```bash
as.bat <pid>
```