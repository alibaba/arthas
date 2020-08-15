
`sysprop`{{execute T2}} 命令可以查看当前JVM的系统属性(`System Property`)

## 使用参考

`sysprop -h`{{execute T2}} 

```
USAGE:
   sysprop [-h] [property-name] [property-value]

 SUMMARY:
   Display, and change all the system properties.

 EXAMPLES:
 sysprop
 sysprop file.encoding
 sysprop production.mode true

 WIKI:
   https://arthas.aliyun.com/doc/sysprop

 OPTIONS:
 -h, --help                                  this help
 <property-name>                             property name
 <property-value>                            property value
```
