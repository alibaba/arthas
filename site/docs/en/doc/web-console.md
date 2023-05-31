# Web Console

[`Web Console` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=case-web-console)

## Connect arthas through the browser

Arthas supports the Web Console. After attach success, the user can access: [http://127.0.0.1:8563/](http://127.0.0.1:8563/).

The user can fill in the IP and connect the remote arthas on other machines.

![](/images/web-console-local.png)

::: warning
By default, arthas only listens to `127.0.0.1`, so if you want to connect from a remote, you can use the `--target-ip` parameter to specify the IP. See the help description for `-h` for more information.
:::

If you have suggestions for the Web Console, please leave a message here: [https://github.com/alibaba/arthas/issues/15](https://github.com/alibaba/arthas/issues/15)

- Copy and paste shortcut keys in Web Console: [https://github.com/alibaba/arthas/issues/1056](https://github.com/alibaba/arthas/issues/1056)

::: tip
Since 3.5.4, you can right-click to copy and paste in the Web Console.
:::

## scrollback URL parameters

::: tip
Since 3.5.5
:::

By default, the number of rows that the Web Console supports to roll back upwards is 1000. It can be specified with `scrollback` in the URL. for example

[http://127.0.0.1:8563/?scrollback=3000](http://127.0.0.1:8563/?scrollback=3000)

## Connect remote arthas through arthas tunnel server

Reference: [Arthas Tunnel](tunnel.md)
