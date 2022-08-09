# Web Console

[`Web Console`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=case-web-console)

## 通过浏览器连接 arthas

Arthas 目前支持 Web Console，用户在 attach 成功之后，可以直接访问：[http://127.0.0.1:8563/](http://127.0.0.1:8563/)。

可以填入 IP，远程连接其它机器上的 arthas。

![](/images/web-console-local.png)

::: warning
默认情况下，arthas 只 listen 127.0.0.1，所以如果想从远程连接，则可以使用 `--target-ip`参数指定 listen 的 IP，更多参考`-h`的帮助说明。
注意会有安全风险，考虑下面的 tunnel server 的方案。
:::

- 在 Web Console 复制粘贴快捷键参考： [https://github.com/alibaba/arthas/issues/1056](https://github.com/alibaba/arthas/issues/1056)

::: tip
3.5.4 版本后，在 Web Console 可以鼠标右键复制粘贴。
:::

## scrollback URL 参数

::: tip
3.5.5 版本后支持
:::

默认 Web Console 支持向上回滚的行数是 1000。可以在 URL 里用`scrollback`指定。比如

[http://127.0.0.1:8563/?scrollback=3000](http://127.0.0.1:8563/?scrollback=3000)

## 使用 arthas tunnel server 连接远程 arthas

参考：[Arthas Tunnel](tunnel.md)
