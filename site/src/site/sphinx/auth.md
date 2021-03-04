auth
===

> 验证当前会话

### 配置用户名和密码

在attach时，可以在命令行指定密码。比如：

```
java -jar arthas-boot.jar --password ppp
```

* 可以通过 `--username` 选项来指定用户，默认值是`arthas`。
* 也可以在 arthas.properties 里中配置 username/password。命令行的优先级大于配置文件。


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

### web console密码验证

打开浏览器，会有弹窗提示需要输入 用户名 和 密码。

成功之后，则可以直接连接上 web console。

### http api验证

Arthas 采用的是 HTTP 标准的 Basic Authorization，客户端请求时增加对应的header即可。

* 参考：[https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)