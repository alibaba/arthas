<img src="https://cdn.jsdelivr.net/gh/shiyindaxiaojie/images/readme/icon.png" align="right" />

[license-apache2.0]:https://www.apache.org/licenses/LICENSE-2.0.html

[github-action]:https://github.com/shiyindaxiaojie/arthas/actions

[sonarcloud-dashboard]:https://sonarcloud.io/dashboard?id=shiyindaxiaojie_arthas

# Arthas 在线诊断工具

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/images/readme/language-java-blue.svg) [![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/images/readme/license-apache2.0-red.svg)][license-apache2.0] [![](https://github.com/shiyindaxiaojie/arthas/actions/workflows/maven-ci.yml/badge.svg?branch=3.6.x)][github-action] [![](https://sonarcloud.io/api/project_badges/measure?project=shiyindaxiaojie_arthas&metric=alert_status)][sonarcloud-dashboard]

Arthas 是阿里巴巴开源的在线诊断工具，提供了 `Dashboard 负载总览`、`Thread 线程占用`、`Stack 堆栈查看`、`Watch 性能观测` 等功能。笔者参考原作者 [@wf2311](https://github.com/wf2311/arthas-ext) 的实现进行了优化：
1. 服务发现：自动获取接入的应用列表 IP 和端口，无须手动输入 AgentId
2. 权限控制：基于 Spring Security 实现登录控制，并支持 Nacos 动态绑定账号与服务

> 本文档只介绍 `arthas-tunnel-server` 项目，其他细节请查阅 [官方文档](https://github.com/alibaba/arthas)。

## 演示图例

### 改造前

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/images/arthas/arthas-dashboard-overview-old.png)

### 改造后

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/images/arthas/arthas-dashboard-overview.png)

登录控制

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/images/arthas/arthas-dashboard-login.png)

配置管理

````properties
# tunnel-server/src/main/resources/application.properties
# 管理员授权
spring.security.users[0].name=admin
spring.security.users[0].password=123456
spring.security.users[0].roles=ADMIN
# 指定服务授权
spring.security.users[1].name=user
spring.security.users[1].password=123456
spring.security.users[1].roles=eden-gateway,eden-demo-cola
````

## 如何构建

本项目默认使用 Maven 来构建，最快的使用方式是 `git clone` 到本地。在项目的根目录执行 `mvn package -T 4C` 完成本项目的构建。

## 如何启动

### IDEA 启动

本项目不依赖外部组件，可以直接启动运行。

1. 在项目目录下运行 `mvn install`（如果不想运行测试，可以加上 `-DskipTests` 参数）。
2. 进入 `tunnel-server` 目录，执行 `mvn spring-boot:run` 或者启动 `ArthasTunnelApplication` 类。运行成功的话，可以看到 `Spring Boot` 启动成功的界面。
3. 进入 `web-ui` 目录，执行 `yarn run dev:tunnel` 或者 `yarn run dev:ui`。运行后控制台日志输出 `http://localhost:8000` 地址，点击访问。

### 镜像启动

本项目已发布到 [Docker Hub](https://hub.docker.com/repository/docker/shiyindaxiaojie/arthas-tunnel-server)，请执行参考命令运行。

```bash
docker run -p 8080:8080 --name=arthas-tunnel-server -d shiyindaxiaojie/arthas-tunnel-server
```

## 如何部署

### FatJar 部署

执行 `mvn clean package` 打包成一个 fat jar，参考如下命令启动编译后的控制台。

```bash
java -Dserver.port=8080 -jar target/arthas-tunnel-server.jar
```

### Docker 部署

本项目使用了 Spring Boot 的镜像分层特性优化了镜像的构建效率，请确保正确安装了 Docker 工具，然后执行以下命令。

```bash
docker build -f Dockerfile-Tunnel -t arthas-tunnel-server:{tag} .
```

### Helm 部署

以应用为中心，建议使用 Helm 统一管理所需部署的 K8s 资源描述文件，请参考以下命令完成应用的安装和卸载。

```bash
helm install arthas-tunnel-server ./helm # 部署资源
helm uninstall arthas-tunnel-server # 卸载资源
```

## 如何接入

为了减少客户端集成的工作，您可以使用 [eden-architect](https://github.com/shiyindaxiaojie/eden-architect) 框架，只需要两步就可以完成 Arthas 的集成。

1. 引入 Arthas 依赖
````xml
<dependency>
    <groupId>io.github.shiyindaxiaojie</groupId>
    <artifactId>eden-arthas-spring-boot-starter</artifactId>
</dependency>
````
2. 开启 Arthas 配置
````yaml
spring:
  arthas: 
    enabled: false # 默认关闭，请按需开启

arthas: # 在线诊断工具
  agent-id: ${spring.application.name}@${random.value}
  tunnel-server: ws://localhost:7777/ws # Arthas 地址
  session-timeout: 1800
  telnet-port: 0 # 随机端口
  http-port: 0 # 随机端口
````

笔者提供了两种不同应用架构的示例，里面有集成 Sentinel 的示例。
* 面向领域模型的 **COLA 架构**，代码实例可以查看 [eden-demo-cola](https://github.com/shiyindaxiaojie/eden-demo-cola)
* 面向数据模型的 **分层架构**，代码实例请查看 [eden-demo-layer](https://github.com/shiyindaxiaojie/eden-demo-layer)

## 变更日志

请查阅 [CHANGELOG.md](https://github.com/shiyindaxiaojie/arthas/blob/3.6.x/CHANGELOG.md)