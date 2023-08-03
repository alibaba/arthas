下面介绍通过`jad`/`mc`/`redefine` 命令实现动态更新代码的功能。

目前，访问 [/user/0]({{TRAFFIC_HOST1_80}}/user/0) ，会返回 500 异常：

下面通过热更新代码，修改这个逻辑。

### jad 反编译 UserController

`jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java`{{execute T2}}

jad 反编译的结果保存在 `/tmp/UserController.java`文件里了。

再打开一个终端于 `Tab 3`，然后在 `Tab3` 里用 `sed` 来编辑`/tmp/UserController.java`：

`sed -i 's/throw new IllegalArgumentException("id < 1")/return new User(id, "name" + id)/g' /tmp/UserController.java`{{execute T3}}

使用 `cat` 命令查看修改后的内容：

`cat /tmp/UserController.java`{{exec}}

比如当 user id 小于 1 时，也正常返回，不抛出异常：

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

### [mc](https://arthas.aliyun.com/doc/mc.html)

(Memory Compiler) 命令来编译加载 UserController
可以通过 -c 指定 classLoaderHash 或者 --classLoaderClass 参数指定 ClassLoader，这里为了操作连贯性使用 classLoaderClass

### 查询 UserController 类加载器

#### sc 查找加载 UserController 的 ClassLoader

回到 `Tab 2` 里运行 `sc -d *UserController | grep classLoaderHash`{{exec}}

#### classloader 查询类加载器名称

`classloader -l`{{exec}} 查询所有的类加载器列表，`UserController classLoaderHash` 值对应的类加载器为 `org.springframework.boot.loader.LaunchedURLClassLoader`

### mc 编译加载 UserController

保存到 `/tmp/UserController.java` 之后可以使用 mc (Memory Compiler) 命令来编译

### mc 指定 classloader 编译 UserController

`mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp`{{exec}}

### [redefine](https://arthas.aliyun.com/doc/redefine.html)

再使用`redefine`命令重新加载新编译好的`UserController.class`：

`redefine /tmp/com/example/demo/arthas/user/UserController.class`{{execute T2}}

```
$ redefine /tmp/com/example/demo/arthas/user/UserController.class
redefine success, size: 1
```

### 热修改代码结果

`redefine`成功之后，再次访问 [/user/0]({{TRAFFIC_HOST1_80}}/user/0) ，结果是：

```
{
  "id": 0,
  "name": "name0"
}
```
