

In this case, the user will resolve the HTTP 401 issue.

Visit: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/admin

The result is:

```
Something went wrong: 401 Unauthorized
```

We know that `401` is usually intercepted by the permission-managed `Filter`, so which `Filter` returns 401?


### Track all Filter methods

Start trace:

`trace javax.servlet.Filter *`{{execute T2}}

Visit: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/admin

At the deepest level of the call tree, you can find `AdminFilterConfig$AdminFilter` which returns `401`:

```
+---[3.806273ms] javax.servlet.FilterChain:doFilter()
|   `---[3.447472ms] com.example.demo.arthas.AdminFilterConfig$AdminFilter:doFilter()
|       `---[0.17259ms] javax.servlet.http.HttpServletResponse:sendError()
```

### Get the call stack through stack command

From the above result, we can find the method: `HttpServletResponse:sendError()`. So we can use `stack` command to resolved the HTTP `401` issue.


Run:

`stack javax.servlet.http.HttpServletResponse sendError 'params[0]==401'`{{execute T2}}

Visit: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/admin

The Result:

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
