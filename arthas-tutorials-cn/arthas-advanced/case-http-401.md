在这个案例里，展示排查 HTTP 401 问题的技巧。

访问： [/admin]({{TRAFFIC_HOST1_80}}/admin)

结果是：

```
Something went wrong: 401 Unauthorized
```

我们知道`401`通常是被权限管理的`Filter`拦截了，那么到底是哪个`Filter`处理了这个请求，返回了 401？

### 跟踪所有的 Filter 函数

开始 trace：

`trace javax.servlet.Filter *`{{execute T2}}

访问： [/admin]({{TRAFFIC_HOST1_80}}/admin)

可以在调用树的最深层，找到`AdminFilterConfig$AdminFilter`返回了`401`

### 通过 stack 获取调用栈

上面是通过`trace`命令来获取信息，从结果里，我们可以知道通过`stack`跟踪`HttpServletResponse:sendError()`，同样可以知道是哪个`Filter`返回了`401`

执行：

`stack javax.servlet.http.HttpServletResponse sendError 'params[0]==401'`{{execute T2}}

访问： [/admin]({{TRAFFIC_HOST1_80}}/admin)
