### Specify classloader

The classloader that loads the class can be found through the `sc`{{}} command.

`sc -d org.springframework.context.ApplicationContext`{{exec}}

After obtaining the hashcode of `org.springframework.boot.loader.LaunchedURLClassLoader` using the above command, you can use the `-c` or `--classloader` parameter to specify it. Here, we use `--classLoaderClass` to specify the classloader:

`vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext`{{execute T2}}
