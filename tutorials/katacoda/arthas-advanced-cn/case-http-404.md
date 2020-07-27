

在这个案例里，展示排查HTTP 404问题的技巧。

访问： https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/a.txt 

结果是：

```
Something went wrong: 404 Not Found
```

那么到底是哪个Servlet处理了这个请求，返回了404？

### 跟踪所有的Servlet函数

开始trace：

`trace javax.servlet.Servlet * > /tmp/servlet.txt`{{execute T2}}

访问： https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/a.txt

在`Terminal 3`里，查看`/tmp/servlet.txt`的内容：

`less /tmp/servlet.txt`{{execute T3}}

`/tmp/servlet.txt`里的内容会比较多，需要耐心找到调用树里最长的地方。

可以发现请求最终是被`freemarker`处理的：

```
`---[13.974188ms] org.springframework.web.servlet.ViewResolver:resolveViewName()
    +---[0.045561ms] javax.servlet.GenericServlet:<init>()
    +---[min=0.045545ms,max=0.074342ms,total=0.119887ms,count=2] org.springframework.web.servlet.view.freemarker.FreeMarkerView$GenericServletAdapter:<init>()
    +---[0.170895ms] javax.servlet.GenericServlet:init()
    |   `---[0.068578ms] javax.servlet.GenericServlet:init()
    |       `---[0.021793ms] javax.servlet.GenericServlet:init()
    `---[0.164035ms] javax.servlet.GenericServlet:getServletContext()
```