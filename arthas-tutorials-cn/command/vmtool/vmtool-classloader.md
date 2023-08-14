### 指定 classloader

可以通过`sc`{{}} 命令查找到加载 class 的 classloader。

`sc -d org.springframework.context.ApplicationContext`{{exec}}

通过上面命令得到 `org.springframework.boot.loader.LaunchedURLClassLoader`{{}} hashcode 之后用`-c`{{}} / `--classloader`{{}} 参数指定，这里使用 `--classLoaderClass ` 来指定 classloader

`vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext`{{execute T2}}
