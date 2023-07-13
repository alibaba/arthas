

In this case, the user will resolve the HTTP 404 issue.

Visit: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/a.txt 

The result is:

```
Something went wrong: 404 Not Found
```

So which servlet is handle this request and returning 404?

### Trace all the Servlet methods

Start trace:

`trace javax.servlet.Servlet * > /tmp/servlet.txt`{{execute T2}}

Visit: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/a.txt

In `Terminal 3`, view the contents of `/tmp/servlet.txt`:

`less /tmp/servlet.txt`{{execute T3}}

The contents of `/tmp/servlet.txt` will be more, and you need to be patient to find the longest level in the call tree.

It can be found that the request is handled by `freemarker`:

```
`---[13.974188ms] org.springframework.web.servlet.ViewResolver:resolveViewName()
    +---[0.045561ms] javax.servlet.GenericServlet:<init>()
    +---[min=0.045545ms,max=0.074342ms,total=0.119887ms,count=2] org.springframework.web.servlet.view.freemarker.FreeMarkerView$GenericServletAdapter:<init>()
    +---[0.170895ms] javax.servlet.GenericServlet:init()
    |   `---[0.068578ms] javax.servlet.GenericServlet:init()
    |       `---[0.021793ms] javax.servlet.GenericServlet:init()
    `---[0.164035ms] javax.servlet.GenericServlet:getServletContext()
```