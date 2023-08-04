Currently, visiting [/user/0]({{TRAFFIC_HOST1_80}}/user/0) will return a 500 error:

But what are the specific parameters of the request, what is the exception stack?

### View the parameters/exception of UserController

Execute in Arthas:

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}'`{{execute T2}}

1. The first argument is the class name, which supports wildcards.
2. The second argument is the function name, which supports wildcards.

Visiting [/user/0]({{TRAFFIC_HOST1_80}}/user/0) , the `watch` command will print the parameters and exception
The user can see that the actual thrown exception is `IllegalArgumentException`.

The user can exit the watch command by typing `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}}.

If the user want to expand the result, can use the `-x` option:

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}' -x 2`{{execute T2}}

### The return value expression

In the above example, the third argument is the `return value expression`, which is actually an `ognl` expression that supports some built-in objects:

- loader
- clazz
- method
- target
- params
- returnObj
- throwExp
- isBefore
- isThrow
- isReturn

You can use these built-in objects in the expressions. For example, return an array:

`watch com.example.demo.arthas.user.UserController * '{params[0], target, returnObj}'`{{execute T2}}

More references: https://arthas.aliyun.com/doc/en/advice-class.html

### The conditional expression

The `watch` command supports conditional expressions in the fourth argument, such as:

`watch com.example.demo.arthas.user.UserController * returnObj 'params[0] > 100'`{{execute T2}}

When visit [/user/1]({{TRAFFIC_HOST1_80}}/user/1) , the `watch` command print nothing.

When visit [/user/101]({{TRAFFIC_HOST1_80}}/user/101) , the `watch` command will print the result.

### Capture when an exception occurs

The `watch` command supports the `-e` option, which means that only requests that throw an exception are caught:

`watch com.example.demo.arthas.user.UserController * "{params[0],throwExp}" -e`{{execute T2}}

### Filter by cost

The watch command supports filtering by cost, such as:

`watch com.example.demo.arthas.user.UserController * '{params, returnObj}' '#cost>200'`{{execute T2}}
