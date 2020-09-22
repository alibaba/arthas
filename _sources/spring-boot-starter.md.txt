Arthas Spring Boot Starter
=====

> 只支持 spring boot 2

最新版本：[查看](https://search.maven.org/search?q=arthas-spring-boot-starter)

配置maven依赖：

```xml
        <dependency>
            <groupId>com.taobao.arthas</groupId>
            <artifactId>arthas-spring-boot-starter</artifactId>
            <version>${arthas.version}</version>
        </dependency>
```

应用启动后，spring会启动arthas，并且attach自身进程。


> 一键创建包含 Arthas Spring Boot Starter 的工程：[点击](http://start.aliyun.com/bootstrap.html/#!dependencies=arthas)

### 配置属性

比如，通过配置tunnel server实现远程管理：

```
arthas.agent-id=hsehdfsfghhwertyfad
arthas.tunnel-server=ws://47.75.156.201:7777/ws
```

全部支持的配置项：[参考](https://github.com/alibaba/arthas/blob/master/arthas-spring-boot-starter/src/main/java/com/alibaba/arthas/spring/ArthasProperties.java)

参考：[Arthas Properties](arthas-properties.md)

### 查看Endpoint信息

> 需要配置spring boot暴露endpoint：[参考](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-endpoints)

假定endpoint端口是 8080，则通过下面url可以查看：

http://localhost:8080/actuator/arthas

```
{
    "arthasConfigMap": {
        "agent-id": "hsehdfsfghhwertyfad",
        "tunnel-server": "ws://47.75.156.201:7777/ws",
    }
}
```

### 非spring boot应用使用方式

非Spring Boot应用，可以通过下面的方式来使用：

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