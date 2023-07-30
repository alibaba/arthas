This case introduces the ability to dynamically update code via the `jad`/`mc`/`redefine` command.

Currently, visiting [/user/0]({{TRAFFIC_HOST1_80}}/user/0) will return a 500 error:

This logic will be modified by `redefine` command below.

### Use jad command to decompile UserController

`jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java`{{execute T2}}

The result of jad command will be saved in the `/tmp/UserController.java` file.

Then open a new terminal in the `Tab 3`, then use `sed` to edit `/tmp/UserController.java`:

`sed -i 's/throw new IllegalArgumentException("id < 1")/return new User(id, "name" + id)/g' /tmp/UserController.java`{{execute T3}}

View the modified content using the `cat` command:

`cat /tmp/UserController.java`{{exec}}

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

### [mc](https://arthas.aliyun.com/en/doc/mc.html)

The (Memory Compiler) command can be used to compile and load the UserController.The classLoaderHash can be specified using the -c flag, or the –classLoaderClass parameter can be used to specify the ClassLoader. Here, for the sake of continuity, the classLoaderClass is used.

### Querying the UserController Class Loader

#### Using the sc command to search for the ClassLoader that loaded the UserController

Go back `Tab 2` and run `sc -d *UserController | grep classLoaderHash`{{exec}}

#### Using the classloader command to query the names of the ClassLoaders

The command `classloader -l`{{exec}} can be used to query a list of all ClassLoaders. The value of `UserController classLoaderHash` corresponds to the `org.springframework.boot.loader.LaunchedURLClassLoader` ClassLoader.

### mc Compiling and Loading UserController

After saving to `/tmp/UserController.java`, it can be compiled using the mc (Memory Compiler) command.

### mc Compiling UserController with a Specified ClassLoader

`mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp`{{exec}}

### [redefine](https://arthas.aliyun.com/en/doc/redefine.html)

Then reload the newly compiled `UserController.class` with the `redefine` command:

`redefine /tmp/com/example/demo/arthas/user/UserController.class`{{execute T2}}

```
$ redefine /tmp/com/example/demo/arthas/user/UserController.class
redefine success, size: 1
```

### Check the results of the hotswap code

After the `redefine` command is executed successfully, visit [/user/0]({{TRAFFIC_HOST1_80}}/user/0) again.

The result is:

```
{
  "id": 0,
  "name": "name0"
}
```
