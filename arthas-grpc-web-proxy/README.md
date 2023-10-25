# Arthas-grpc
项目启动流程:

## 1. grpc-web代理服务配置
1. 前端grpc-web请求ip和port配置: [配置文件](./ui/src/main.js)
    ```js
      app.use(ViewUIPlus)
        .use(router)
        .provide("apiHost","http://localhost:8567")
        .mount('#app')
    ```
2. 后端端口配置: [配置文件](./src/main/java/com/taobao/arthas/grpcweb/grpc/DemoBootstrap.java), 修改``GRPC_WEB_PROXY_PORT``变量,即可配置grpc-web代理服务端口。<br><br>
   若需要配置grpc服务端口和http页面服务端口, 分别修改`GRPC_PORT`和`HTTP_PORT`即可<br><br>
  *注意, 前后端grpc-web代理服务端口需一致(默认使用端口号: 8567) 
## 2. 项目编译

```shell
mvn compile
```

## 3. 项目运行

启动 [com.taobao.arthas.grpcweb.grpc.DemoBootstrap](./src/main/java/com/taobao/arthas/grpcweb/grpc/DemoBootstrap.java)

## 4. 页面访问
启动后,命令行终端会打印出访问地址
```text
Open your web browser and navigate to http://127.0.0.1:{http_port}/index.html
```

# netty grpc web proxy

本项目中使用到的grpc-web代理服务

from: https://github.com/grpc/grpc-web/tree/1.4.2/src/connector

原项目已废弃删除，本项目改用 netty 来做转发。

## 缺点

原项目需要 `.proto` 文件编译的 `.class`才能运行，比如`GreeterGrpc`，本项目同样有这个问题。


## 测试

工程导入IDE之后,进入test目录

在 com.taobao.arthas.grpcweb.proxy.server.GrpcWebProxyServerTest 启动测试

也可以用原项目的相关工程来测试

* https://github.com/grpc/grpc-web/

## 开发验证

可以用其它的 grpc web proxy来抓包辅助验证。

## 用 envoy

下载envoy 后，可以用本项目里的`envoy.yaml`

* `envoy --config-path ./envoy.yaml`

## 使用 grpcwebproxy 

* https://github.com/improbable-eng/grpc-web/blob/master/go/grpcwebproxy/README.md

下载后，启动：

* `grpcwebproxy --backend_addr 127.0.0.1:9090 --run_tls_server=false --allow_all_origins`


