auth
===

> 验证当前会话

### 配置用户名和密码

在attach时，可以在命令行指定密码。比如：

```
java -jar arthas-boot.jar --password ppp
```

* 可以通过 `--username` 选项来指定用户，默认值是`arthas`。
* 也可以在 `arthas.properties` 里中配置 username/password。命令行的优先级大于配置文件。
* 如果只配置`username`，没有配置`password`，则会生成随机密码，打印在`~/logs/arthas/arthas.log`中

  ```
  Using generated security password: 0vUBJpRIppkKuZ7dYzYqOKtranj4unGh
  ```

### 在telnet console里鉴权

连接到arthas后，直接执行命令会提示需要鉴权：

```bash
[arthas@37430]$ help
Error! command not permitted, try to use 'auth' command to authenticates.
```

使用`auth`命令来鉴权，成功之后可以执行其它命令。

```
[arthas@37430]$ auth ppp
Authentication result: true
```

* 可以通过 `--username` 选项来指定用户，默认值是`arthas`。


### Web console密码验证

打开浏览器，会有弹窗提示需要输入 用户名 和 密码。

成功之后，则可以直接连接上 web console。

### HTTP API 验证


#### Authorization Header方式（推荐）
Arthas 采用的是 HTTP 标准的 Basic Authorization，客户端请求时增加对应的header即可。

* 参考：[https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)


例如，用户名是：`admin`，密码是 `admin`，则组合为字符串： `admin:admin`，base64结果是： `YWRtaW46YWRtaW4=`，则HTTP 请求增加`Authorization` header：

```bash
curl 'http://localhost:8563/api' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  --data-raw '{"action":"exec","command":"version"}' 
```

#### URL 参数传递方式

为了方便各种特殊情况，支持了以 parameters 方式传递username和password。比如：

```bash
curl 'http://localhost:8563/api?password=admin' \
  --data-raw '{"action":"exec","command":"version"}' 
```

