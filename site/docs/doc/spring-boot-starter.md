# Arthas Spring Boot Starter

::: tip
arthas 3.7.2及以后的版本同时支持 springboot 2/3
:::

最新版本：[查看](https://search.maven.org/search?q=arthas-spring-boot-starter)

配置 maven 依赖：

```xml
        <dependency>
            <groupId>com.taobao.arthas</groupId>
            <artifactId>arthas-spring-boot-starter</artifactId>
            <version>${arthas.version}</version>
        </dependency>
```

应用启动后，spring 会启动 arthas，并且 attach 自身进程。

::: tip
一键创建包含 Arthas Spring Boot Starter 的工程：<a href="https://start.aliyun.com/bootstrap.html/#!dependencies=arthas" target="_blank">点击</a>
:::

## 配置属性

比如，通过配置 tunnel server 实现远程管理：

```
arthas.agent-id=hsehdfsfghhwertyfad
arthas.tunnel-server=ws://47.75.156.201:7777/ws
```

全部支持的配置项：[参考](https://github.com/alibaba/arthas/blob/master/arthas-spring-boot-starter/src/main/java/com/alibaba/arthas/spring/ArthasProperties.java)

::: tip
默认情况下，arthas-spring-boot-starter 会禁掉`stop`命令。
:::

参考：[Arthas Properties](arthas-properties.md)

## 查看 Endpoint 信息

::: tip
需要配置 spring boot 暴露 endpoint：[参考](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-endpoints)
:::

假定 endpoint 端口是 8080，则通过下面 url 可以查看：

http://localhost:8080/actuator/arthas

```
{
    "arthasConfigMap": {
        "agent-id": "hsehdfsfghhwertyfad",
        "tunnel-server": "ws://47.75.156.201:7777/ws",
    }
}
```

## 非 spring boot 应用使用方式

非 Spring Boot 应用，可以通过下面的方式来使用：

```xml
        <dependency>
            <groupId>com.taobao.arthas</groupId>
            <artifactId>arthas-agent-attach</artifactId>
            <version>${arthas.version}</version>
        </dependency>
        <dependency>
            <groupId>com.taobao.arthas</groupId>
            <artifactId>arthas-packaging</artifactId>
            <version>${arthas.version}</version>
        </dependency>
```

```java
import com.taobao.arthas.agent.attach.ArthasAgent;

public class ArthasAttachExample {

	public static void main(String[] args) {
		ArthasAgent.attach();
	}

}
```

也可以配置属性：

```java
        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put("arthas.appName", "demo");
        configMap.put("arthas.tunnelServer", "ws://127.0.0.1:7777/ws");
        ArthasAgent.attach(configMap);
```

::: warning
注意配置必须是`驼峰`的，和 spring boot 的`-`风格不一样。spring boot 应用才同时支持`驼峰` 和 `-`风格的配置。
:::
