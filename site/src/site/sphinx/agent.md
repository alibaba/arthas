以Java Agent的方式启动
===

通常Arthas是以动态attach的方式来诊断应用，但从`3.2.0`版本起，Arthas支持直接以 java agent的方式启动。

比如下载全量的arthas zip包，解压之后以 `-javaagent` 的参数指定`arthas-agent.jar`来启动：

```
java -javaagent:/tmp/test/arthas-agent.jar -jar math-game.jar
```

默认的配置项在解压目录里的`arthas.properties`文件里。参考：[Arthas Properties](arthas-properties.md)


Java Agent机制参考： https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html