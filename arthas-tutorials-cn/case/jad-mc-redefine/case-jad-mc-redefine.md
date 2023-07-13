下面介绍通过`jad`/`mc`/`redefine` 命令实现动态更新代码的功能。

目前，访问 http://localhost/user/0 ，会返回500异常：

`curl http://localhost/user/0`{{execute T3}}

```
{"timestamp":1550223186170,"status":500,"error":"Internal Server Error","exception":"java.lang.IllegalArgumentException","message":"id < 1","path":"/user/0"}
```

下面通过热更新代码，修改这个逻辑。

### jad反编译UserController

`jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java`{{execute T2}}

jad反编译的结果保存在 `/tmp/UserController.java`文件里了。

再打开一个`Terminal 3`，然后用vim来编辑`/tmp/UserController.java`：

`vim /tmp/UserController.java`{{execute T3}}

比如当 user id 小于1时，也正常返回，不抛出异常：

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

### sc查找加载UserController的ClassLoader

`sc -d *UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d *UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

可以发现是 spring boot `LaunchedURLClassLoader@1be6f5c3` 加载的。

注意hashcode是变化的，需要先查看当前的ClassLoader信息，提取对应ClassLoader的hashcode。

如果你使用`-c`，你需要手动输入hashcode：`-c <hashcode>`

对于只有唯一实例的ClassLoader可以通过`--classLoaderClass`指定class name，使用起来更加方便.

`--classLoaderClass` 的值是ClassLoader的类名，只有匹配到唯一的ClassLoader实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

### mc

保存好`/tmp/UserController.java`之后，使用`mc`(Memory Compiler)命令来编译，并且通过`--classLoaderClass`参数指定ClassLoader：

`mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp`{{execute T2}}

```bash
$ mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp
Memory compiler output:
/tmp/com/example/demo/arthas/user/UserController.class
Affect(row-cnt:1) cost in 346 ms
```

也可以通过`mc -c <classLoaderHash> /tmp/UserController.java -d /tmp`，使用`-c`参数指定ClassLoaderHash:

```bash
$ mc -c 1be6f5c3 /tmp/UserController.java -d /tmp
```

### redefine

再使用`redefine`命令重新加载新编译好的`UserController.class`：

`redefine /tmp/com/example/demo/arthas/user/UserController.class`{{execute T2}}

```
$ redefine /tmp/com/example/demo/arthas/user/UserController.class
redefine success, size: 1
```

### 热修改代码结果

`redefine`成功之后，再次访问 https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/0 ，结果是：

```
{
  "id": 0,
  "name": "name0"
}
```
