
`jvm`{{execute T2}} 命令可以查看当前JVM信息。

## 查找Java应用的classpath

`jvm | grep PATH`{{execute T2}}

```bash
[arthas@41064]$ jvm | grep PATH
 CLASS-PATH                             packaging/target/arthas-bin/math-game.jar
 BOOT-CLASS-PATH                        /Library/Java/JavaVirtualMachines/jdk1.8.0_151.jdk/Contents/Home/jre/lib/resources.jar:/Librar
 LIBRARY-PATH                           /Users/gongdewei/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library/Java/Extens
```
