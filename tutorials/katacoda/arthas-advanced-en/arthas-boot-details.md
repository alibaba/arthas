

`arthas-boot.jar` supports many parameters and can be viewed by `java -jar arthas-boot.jar -h`{{execute T2}}.

## Allow external network access

By default, the arthas server listens for the IP of `127.0.0.1`. If you want remote access, you can use the `--target-ip` option.

`java -jar arthas-boot.jar --target-ip`{{execute T2}}


## List all versions


`java -jar arthas-boot.jar --versions`{{execute T2}}

Use the specified version:

`java -jar arthas-boot.jar --use-version 3.1.0`{{execute T2}}

## Only listens at the Telnet port and does not listen at the HTTP port.

`java -jar arthas-boot.jar --telnet-port 9999 --http-port -1`{{execute T2}}

## Print verbose information

`java -jar arthas-boot.jar -v`{{execute T2}}



