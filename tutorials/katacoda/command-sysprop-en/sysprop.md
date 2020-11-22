
The `sysprop`{{execute T2}} command allows you to examine the system properties from the target JVM.

## Usage

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
