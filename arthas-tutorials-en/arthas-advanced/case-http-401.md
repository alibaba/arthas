In this case, the user will resolve the HTTP 401 issue.

Visit: [/admin]({{TRAFFIC_HOST1_80}}/admin)

The result is:

```
Something went wrong: 401 Unauthorized
```

We know that `401` is usually intercepted by the permission-managed `Filter`, so which `Filter` returns 401?

### Track all Filter methods

Start trace:

`trace javax.servlet.Filter *`{{execute T2}}

Visit: [/admin]({{TRAFFIC_HOST1_80}}/admin)

At the deepest level of the call tree, you can find `AdminFilterConfig$AdminFilter` which returns `401`

### Get the call stack through stack command

From the above result, we can find the method: `HttpServletResponse:sendError()`. So we can use `stack` command to resolved the HTTP `401` issue.

Run:

`stack javax.servlet.http.HttpServletResponse sendError 'params[0]==401'`{{execute T2}}

Visit: [/admin]({{TRAFFIC_HOST1_80}}/admin)
