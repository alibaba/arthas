<img src="https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/readme/icon.png" align="right" />

[license-apache2.0]:https://www.apache.org/licenses/LICENSE-2.0.html

[github-action]:https://github.com/shiyindaxiaojie/eden-demo-cola/actions

[sonarcloud-dashboard]:https://sonarcloud.io/dashboard?id=shiyindaxiaojie_eden-demo-cola

# Arthas 在线诊断工具

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/readme/language-java-blue.svg) [![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/readme/license-apache2.0-red.svg)][license-apache2.0] [![](https://github.com/shiyindaxiaojie/arthas/workflows/build/badge.svg)][github-action] [![](https://sonarcloud.io/api/project_badges/measure?project=shiyindaxiaojie_arthas&metric=alert_status)][sonarcloud-dashboard]

Arthas 是阿里巴巴开源的在线诊断工具，提供了 `Dashboard 负载总览`、`Thread 线程占用`、`Stack 堆栈查看`、`Watch 性能观测` 等功能。笔者参考原作者 [@wf2311](https://github.com/wf2311/arthas-ext) 的实现进行了优化：
1. 服务发现：自动获取接入的应用列表 IP 和端口，无须手动输入 AgentId
2. 权限控制：基于 Spring Security 实现登录控制，并支持 Nacos 动态绑定账号与服务

> 本文档只介绍 `arthas-tunnel-proxy` 项目，其他细节请查阅 [官方文档](https://github.com/alibaba/arthas)。

## 演示图例

### 改造前

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/arthas/arthas-dashboard-overview-old.png)

### 改造后

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/arthas/arthas-dashboard-overview.png)

登录控制

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/arthas/arthas-dashboard-login.png)

配置管理

````yaml
arthas:
  tunnel:
    users:
      - name: admin
        password: 123456
        roles: '*' # 全部授权
      - name: user
        password: 123456
        roles:
          - eden-gateway # 特定服务授权
````

## 如何构建

本项目默认使用 Maven 来构建，最快的使用方式是 `git clone` 到本地。在项目的根目录执行 `mvn package -T 4C` 完成本项目的构建。

## 如何启动

本项目不依赖外部组件，可以直接启动运行。

1. 在项目目录下运行 `mvn install`（如果不想运行测试，可以加上 `-DskipTests` 参数）。
2. 进入 `tunnel-proxy` 目录，执行 `mvn spring-boot:run` 或者启动 `ArthasProxyApplication` 类。运行成功的话，可以看到 `Spring Boot` 启动成功的界面。

## 如何部署

### Docker 部署

调整 Maven 配置文件 `setiings.xml`，填充
````xml
<settings>
    <profiles>
        <profile>
            <id>github</id>
            <properties>
                <docker.username>${env.DOCKER_USERNAME}</docker.username>
                <docker.password>${env.DOCKER_PASSWORD}</docker.password>
                <docker.image>${env.DOCKER_IMAGE}</docker.image>
            </properties>
        </profile>
    </profiles>
</settings>
````

在项目根目录执行 `mvn -Pgithub -pl tunnel-proxy jib:build -Djib.disableUpdateChecks=true` 打包为镜像。

### Helm 部署

进入 `helm` 目录，执行 `helm install -n arthas arthas .` 安装，在 K8s 环境将自动创建 Arthas 所需的资源文件。

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