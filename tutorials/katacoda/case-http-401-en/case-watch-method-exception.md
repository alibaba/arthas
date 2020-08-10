

Currently, visiting http://localhost/user/0 will return a 500 error:

`curl http://localhost/user/0`{{execute T3}}

```
{"timestamp":1550223186170,"status":500,"error":"Internal Server Error","exception":"java.lang.IllegalArgumentException","message":"id < 1","path":"/user/0"}
```

But what are the specific parameters of the request, what is the exception stack?

### View the parameters/exception of UserController

Execute in Arthas:

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}'`{{execute T2}}


1. The first argument is the class name, which supports wildcards.
2. The second argument is the function name, which supports wildcards.

Visit `curl http://localhost/user/0`{{execute T3}} , the `watch` command will print the parameters and exception

```bash
$ watch com.example.demo.arthas.user.UserController * '{params, throwExp}'
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:2) cost in 53 ms.
ts=2019-02-15 01:35:25; [cost=0.996655ms] result=@ArrayList[
    @Object[][isEmpty=false;size=1],
    @IllegalArgumentException[java.lang.IllegalArgumentException: id < 1],
]
```


The user can see that the actual thrown exception is `IllegalArgumentException`.

The user can exit the watch command by typing `Q`{{execute T2}} or `Ctrl+C`.

If the user want to expand the result, can use the `-x` option:

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}' -x 2`{{execute T2}}

### The return value expression

In the above example, the third argument is the `return value expression`, which is actually an `ognl` expression that supports some built-in objects:

* loader
* clazz
* method
* target
* params
* returnObj
* throwExp
* isBefore
* isThrow
* isReturn

You can use these built-in objects in the expressions. For example, return an array:

`watch com.example.demo.arthas.user.UserController * '{params[0], target, returnObj}'`{{execute T2}}


More references: https://arthas.aliyun.com/doc/en/advice-class.html


### The conditional expression

The `watch` command supports conditional expressions in the fourth argument, such as:

`watch com.example.demo.arthas.user.UserController * returnObj 'params[0] > 100'`{{execute T2}}

When visit https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/1 , the `watch` command print nothing.

When visit https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/101 , the `watch` command will print:

```bash
$ watch com.example.demo.arthas.user.UserController * returnObj 'params[0] > 100'
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:2) cost in 47 ms.
ts=2019-02-13 19:42:12; [cost=0.821443ms] result=@User[
    id=@Integer[101],
    name=@String[name101],
]
```

### Capture when an exception occurs

The `watch` command supports the `-e` option, which means that only requests that throw an exception are caught:

`watch com.example.demo.arthas.user.UserController * "{params[0],throwExp}" -e`{{execute T2}}


### Filter by cost

The watch command supports filtering by cost, such as:

`watch com.example.demo.arthas.user.UserController * '{params, returnObj}' '#cost>200'`{{execute T2}}