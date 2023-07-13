

在这个案例里，展示排查HTTP 401问题的技巧。

访问： https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/admin

结果是：

```
Something went wrong: 401 Unauthorized
```

我们知道`401`通常是被权限管理的`Filter`拦截了，那么到底是哪个`Filter`处理了这个请求，返回了401？


### 跟踪所有的Filter函数

开始trace：

`trace javax.servlet.Filter *`{{execute T2}}

访问： https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/admin

可以在调用树的最深层，找到`AdminFilterConfig$AdminFilter`返回了`401`：

```
+---[3.806273ms] javax.servlet.FilterChain:doFilter()
|   `---[3.447472ms] com.example.demo.arthas.AdminFilterConfig$AdminFilter:doFilter()
|       `---[0.17259ms] javax.servlet.http.HttpServletResponse:sendError()
```

### 通过stack获取调用栈

上面是通过`trace`命令来获取信息，从结果里，我们可以知道通过`stack`跟踪`HttpServletResponse:sendError()`，同样可以知道是哪个`Filter`返回了`401`

执行：

`stack javax.servlet.http.HttpServletResponse sendError 'params[0]==401'`{{execute T2}}

访问： https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/admin

```bash
$ stack javax.servlet.http.HttpServletResponse sendError 'params[0]==401'
Press Q or Ctrl+C to abort.
Affect(class-cnt:2 , method-cnt:4) cost in 87 ms.
ts=2019-02-15 16:44:06;thread_name=http-nio-8080-exec-6;id=16;is_daemon=true;priority=5;TCCL=org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedWebappClassLoader@8546cd5
    @org.apache.catalina.connector.ResponseFacade.sendError()
        at com.example.demo.arthas.AdminFilterConfig$AdminFilter.doFilter(AdminFilterConfig.java:38)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
```
