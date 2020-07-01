


There are some switches in Arthas that can be viewed with the `options`{{execute T2}} command.


View the value of a single option, such as

`options unsafe`{{execute T2}}


## Allow to enhance the classes of JDK

By default, `unsafe` is false, ie commands such as `watch`/`trace` do not enhance the JVM class, which is the class starting with `java.*`.

To enhance the classes in the JVM, execute `options unsafe true`{{execute T2}} to set `unsafe` to true.

## Print objects in JSON format

When `json-format` is false, the output is:

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/usr/lib/jvm/java-8-oracle/jre],
    @String[Java(TM) SE Runtime Environment],
]
```

`options json-format true`{{execute T2}}

When `json-format` is true, the output is:

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#v["/usr/lib/jvm/java-8-oracle/jre","Java(TM) SE Runtime Environment"]
```