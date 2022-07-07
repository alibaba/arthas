Web Console
===

[`Web Console`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=case-web-console)

### 通过浏览器连接arthas


Arthas目前支持Web Console，用户在attach成功之后，可以直接访问：[http://127.0.0.1:8563/](http://127.0.0.1:8563/)。

可以填入IP，远程连接其它机器上的arthas。

![](/images/web-console-local.png)

::: warning
默认情况下，arthas只listen 127.0.0.1，所以如果想从远程连接，则可以使用 `--target-ip`参数指定listen的IP，更多参考`-h`的帮助说明。
注意会有安全风险，考虑下面的tunnel server的方案。
:::

* 在Web Console复制粘贴快捷键参考： [https://github.com/alibaba/arthas/issues/1056](https://github.com/alibaba/arthas/issues/1056)

::: tip
3.5.4 版本后，在Web Console可以鼠标右键复制粘贴。
:::

### scrollback URL参数

::: tip
3.5.5 版本后支持
:::

默认Web Console支持向上回滚的行数是1000。可以在URL里用`scrollback`指定。比如

[http://127.0.0.1:8563/?scrollback=3000](http://127.0.0.1:8563/?scrollback=3000)
### 使用arthas tunnel server连接远程arthas

参考：[Arthas Tunnel](tunnel.md)