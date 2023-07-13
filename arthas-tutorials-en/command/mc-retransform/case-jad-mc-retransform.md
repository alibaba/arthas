This case introduces the ability to dynamically update code via the `jad`/`mc`/`retransform` command.

Currently, visiting http://localhost/user/0 will return a 500 error:

`curl http://localhost/user/0`{{execute T3}}

```
{"timestamp":1550223186170,"status":500,"error":"Internal Server Error","exception":"java.lang.IllegalArgumentException","message":"id < 1","path":"/user/0"}
```

This logic will be modified by `retransform` command below.

## Use jad command to decompile UserController

`jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java`{{execute T2}}

The result of jad command will be saved in the `/tmp/UserController.java` file.


Then open `Terminal 3`, use `vim` to edit `/tmp/UserController.java`:

`vim /tmp/UserController.java`{{execute T3}}

For example, when the user id is less than 1, it also returns normally without throwing an exception:

```java
    @GetMapping(value={"/user/{id}"})
    public User findUserById(@PathVariable Integer id) {
        logger.info("id: {}", (Object)id);
        if (id != null && id < 1) {
			return new User(id, "name" + id);
            // throw new IllegalArgumentException("id < 1");
        }
        return new User(id.intValue(), "name" + id);
    }
```

### Use sc command to find the ClassLoader that loads the UserController

`sc -d *UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d *UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

It can be found that it is loaded by spring boot `LaunchedURLClassLoader@1be6f5c3`.

Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.

For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

### mc

After saving `/tmp/UserController.java`, compile with the `mc` (Memory Compiler) command and specify the ClassLoader with the `--classLoaderClass` option:

`mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp`{{execute T2}}

```bash
$ mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp
Memory compiler output:
/tmp/com/example/demo/arthas/user/UserController.class
Affect(row-cnt:1) cost in 346 ms
```

You can also execute `mc -c <classLoaderHash> /tmp/UserController.java -d /tmp`，using `-c` to specify ClassLoaderHash:

```bash
$ mc -c 1be6f5c3 /tmp/UserController.java -d /tmp
```

### retransform

Then reload the newly compiled `UserController.class` with the `retransform` command:

`retransform /tmp/com/example/demo/arthas/user/UserController.class`{{execute T2}}

```
$ retransform /tmp/com/example/demo/arthas/user/UserController.class
retransform success, size: 1
```

### Check the results of the hotswap code

After the `retransform` command is executed successfully, visit https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/0 again.

The result is:

```
{
  "id": 0,
  "name": "name0"
}
```
