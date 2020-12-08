Arthas Properties
===

The `arthas.properties` file is in the arthas directory.

* If it is automatically downloaded arthas, the directory is under `~/.arthas/lib/3.x.x/arthas/`
* If it is a downloaded complete package, under the decompression directory of arthas

## Supported configuration items

> Note that the configuration must be `camel case`, which is different from the `-` style of spring boot. Only the spring boot application supports both `camel case` and `-` style configuration.

```
#arthas.config.overrideAll=true
arthas.telnetPort=3658
arthas.httpPort=8563
arthas.ip=127.0.0.1

# seconds
arthas.sessionTimeout=1800

#arthas.appName=demoapp
#arthas.tunnelServer=ws://127.0.0.1:7777/ws
#arthas.agentId=mmmmmmyiddddd
```


* If the configuration of `arthas.telnetPort` is -1, the telnet port will not be listened. `arthas.httpPort` is similar.
* If you configure `arthas.telnetPort` to 0, then random listen telnet port, you can find the random port log in `~/logs/arthas/arthas.log`. `arthas.httpPort` is similar.

> If you want to prevent multiple arthas port conflicts on a machine. It can be configured as a random port, or configured as -1, and use arthas through the tunnel server.

## Configured order

The order of configuration is: command line parameters > System Env > System Properties > arthas.properties.

such as:

* `./as.sh --telnet-port 9999` command line configuration will overwrite the default value `arthas.telnetPort=3658` in `arthas.properties`.
* If the application itself sets system properties `arthas.telnetPort=8888`, it will override the default value `arthas.telnetPort=3658` in `arthas.properties`.

If you want `arthas.properties` to have the highest order, you can configure `arthas.config.overrideAll=true`.


