

比如查看`sysprop`命令的帮助信息：

`help sysprop`{{execute T2}} 

```bash
[arthas@50759]$ help sysprop
 USAGE:
   sysprop [-h] [property-name] [property-value]

 SUMMARY:
   Display, and change the system properties.

 EXAMPLES:
   sysprop
   sysprop file.encoding
   sysprop production.mode true

 WIKI:
   https://arthas.aliyun.com/doc/sysprop

 OPTIONS:
 -h, --help                                                           this help
 <property-name>                                                      property name
 <property-value>                                                     property value
```

可以看到具体的命令的参数，示例和WKI地址。

