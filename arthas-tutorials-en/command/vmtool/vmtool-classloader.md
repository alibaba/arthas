### Specify classloader name

`vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext`{{execute T2}}

### Specify classloader hash

The classloader that loads the class can be found through the `sc` command.

Then use the `-c`/`--classloader` parameter to specify:

```bash
vmtool --action getInstances -c 19469ea2 --className org.springframework.context.ApplicationContext
```
