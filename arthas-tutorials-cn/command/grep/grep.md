类似传统的 [grep 命令](https://arthas.aliyun.com/doc/grep.html)。

`grep -h`{{execute T2}}

## 示例命令

### 匹配展示符合范本样式的项

`sysprop | grep java`{{execute T2}}

### `-n`命令显示行号：

`sysprop | grep java -n`{{execute T2}}

### `-v`展示非匹配

`sysenv | grep -v JAVA`{{execute T2}}

### `-e`使用正则表达式匹配，`-m`设定最大展示条数，

`sysenv | grep -e "(?i)(JAVA|sun)" -m 3 -C 2`{{execute T2}}

`thread | grep -m 10 -e "TIMED_WAITING|WAITING"`{{execute T2}}

### 除了显示符合范本样式的那一行之外，`-A`指定显示该行之后的内容，`-B`指定显示该行之前的内容。

`sysenv | grep JAVA -A2 -B3`{{execute T2}}
