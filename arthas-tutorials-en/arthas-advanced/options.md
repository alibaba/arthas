There are some switches in Arthas that can be viewed with the `options`{{execute T2}} command - [options command Docs](https://arthas.aliyun.com/en/doc/options.html).

View the value of a single option, such as

`options unsafe`{{execute T2}}

## Allow to enhance the classes of JDK

By default, `unsafe` is false, ie commands such as `watch`/`trace` do not enhance the JVM class, which is the class starting with `java.*`.

To enhance the classes in the JVM, execute `options unsafe true`{{execute T2}} to set `unsafe` to true.

## Print objects in JSON format

`options json-format`{{exec}} command returned the current json-format value as false.  
When you execute the following command, `ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'`{{exec}}, the result is not in JSON format.

To output the result in JSON format, you can use `options json-format true`{{exec}} to enable it. After enabling it, if you run `ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'`{{exec}} again, you will notice that the output format has been changed to JSON format.
