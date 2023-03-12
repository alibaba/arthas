# Arthas 在线诊断工具

Arthas 是阿里巴巴开源的在线诊断工具，提供了 `Dashboard负载总览`、`Thread线程占用`、`Stack堆栈查看`、`Watch性能观测` 等功能。在实际的生产需求中，笔者参考 [@wf2311](https://github.com/wf2311/arthas-ext) 的实现进行了部分扩展：
1. 应用服务发现
2. 支持权限控制

## 服务端概览

### 改造前

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/arthas/arthas-dashboard-overview-old.png)

### 改造后

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/arthas/arthas-dashboard-overview.png)

新增权限控制

![](https://cdn.jsdelivr.net/gh/shiyindaxiaojie/eden-images/arthas/arthas-dashboard-login.png)

## 客户端集成

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
  tunnel-server: ws://localhost:7777/ws
  session-timeout: 1800
  telnet-port: 0 # 随机端口
  http-port: 0 # 随机端口
````

笔者提供了两种不同应用架构的示例，里面有集成 Sentinel 的示例。
* 面向领域模型的 **COLA 架构**，代码实例可以查看 [eden-demo-cola](https://github.com/shiyindaxiaojie/eden-demo-cola)
* 面向数据模型的 **分层架构**，代码实例请查看 [eden-demo-layer](https://github.com/shiyindaxiaojie/eden-demo-layer)

## 变更日志

请查阅 [CHANGELOG.md](https://github.com/shiyindaxiaojie/arthas/blob/3.6.x/CHANGELOG.md)