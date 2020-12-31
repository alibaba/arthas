Arthas Spring Boot Starter
=====

> Support spring boot 2

Latest Version: [View](https://search.maven.org/search?q=arthas-spring-boot-starter)

Add maven dependency:

```xml
        <dependency>
            <groupId>com.taobao.arthas</groupId>
            <artifactId>arthas-spring-boot-starter</artifactId>
            <version>${arthas.version}</version>
        </dependency>
```

When the application is started, spring will start arthas and attach its own process.


### Configuration properties

For example, by configuring the tunnel server for remote management.

```
arthas.agent-id=hsehdfsfghhwertyfad
arthas.tunnel-server=ws://47.75.156.201:7777/ws
```

All supported configuration: [Reference](https://github.com/alibaba/arthas/blob/master/arthas-spring-boot-starter/src/main/java/com/alibaba/arthas/spring/ArthasProperties.java)

Reference: [Arthas Properties](arthas-properties.md)

### View Endpoint Information

> Need to configure spring boot to expose endpoint: [Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-endpoints).

Assuming the endpoint port is 8080, it can be viewed via the following url.

http://localhost:8080/actuator/arthas

```js
{
    "arthasConfigMap": {
        "agent-id": "hsehdfsfghhwertyfad",
        "tunnel-server": "ws://47.75.156.201:7777/ws",
    }
}
```

### Non-spring boot application usage

Non-Spring Boot applications can be used in the following ways.

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


You can also configure properties:

```java
        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put("arthas.appName", "demo");
        configMap.put("arthas.tunnelServer", "ws://127.0.0.1:7777/ws");
        ArthasAgent.attach(configMap);
```

> Note that the configuration must be `camel case`, which is different from the `-` style of spring boot. Only the spring boot application supports both `camel case` and `-` style configuration.