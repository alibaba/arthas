The following describes the usage of the `classloader` command.

First visit the jsp page: [visite hello jsp page]({{TRAFFIC_HOST1_80}}/hello)

### List all ClassLoaders

`classloader -l`{{execute T2}}

The number of classes loaded by TomcatEmbeddedWebappClassLoader is 0, so in spring boot embedded tomcat, it is just an empty ClassLoader, all the classes are loaded by `LaunchedURLClassLoader`

### List all classes loaded in ClassLoader

List all classes loaded by `org.apache.jasper.servlet.JasperLoader`:

`classloader -a --classLoaderClass org.apache.jasper.servlet.JasperLoader | grep hello`{{exec}}

### Check the structure of classloader

`sc -d org.apache.jsp.jsp.hello_jsp`{{execute T2}}

### View the ClassLoader tree

`classloader -t`{{execute T2}}

### Show the URLs of the URLClassLoader

For example, if you have found that the LaunchedURLClassLoader for Spring is `org.springframework.boot.loader.LaunchedURLClassLoader`, you can use the `-c <hashcode>` parameter to specify the class loader. Alternatively, you can use the `--classLoaderClass` option to specify the class name and view the actual URLs of the URLClassLoader.

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`

### Load the resource file in the specified ClassLoader

Load the specified resource file: `classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`

Use the classloader to load .class resource

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r java/lang/String.class`
