# 以 Java Agent 的方式启动

通常 Arthas 是以动态 attach 的方式来诊断应用，但从`3.2.0`版本起，Arthas 支持直接以 java agent 的方式启动。

比如下载全量的 arthas zip 包，解压之后以 `-javaagent` 的参数指定`arthas-agent.jar`来启动：

```
java -javaagent:/tmp/test/arthas-agent.jar -jar math-game.jar
```

默认的配置项在解压目录里的`arthas.properties`文件里。参考：[Arthas Properties](arthas-properties.md)

Java Agent 机制参考： https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html
