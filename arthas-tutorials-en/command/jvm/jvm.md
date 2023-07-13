
The `jvm`{{execute T2}} command allows you to check the current JVM’s info.

## Search for current java application's classpath

`jvm | grep PATH`{{execute T2}}

```bash
[arthas@41064]$ jvm | grep PATH
 CLASS-PATH                             packaging/target/arthas-bin/math-game.jar
 BOOT-CLASS-PATH                        /Library/Java/JavaVirtualMachines/jdk1.8.0_151.jdk/Contents/Home/jre/lib/resources.jar:/Librar
 LIBRARY-PATH                           /Users/gongdewei/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library/Java/Extens
```
