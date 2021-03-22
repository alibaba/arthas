
通过Http api查询Java应用的System properties，提取`java.class.path`的值。

`json_data=$(curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"exec",
  "command":"sysprop"
}')`{{execute T3}}

* 使用`sed`提取值：

`class_path=$(echo $json_data | tr -d '\n' | sed 's/.*"java.class.path":"\([^"]*\).*/\1/')
echo "classpath: $class_path"`{{execute T3}}

* 使用`json_pp/awk`提取值

`class_path=$(echo $json_data | tr -d '\n' | json_pp | grep java.class.path | awk -F'"' '{ print $4 }')
echo "classpath: $class_path"`{{execute T3}}

输出内容：

```
classpath: math-game.jar
```

注意：

* `echo $json_data | tr -d '\n'` :  删除换行符(`line.separator`的值)，避免影响`sed`/`json_pp`命令处理。
* `awk -F'"' '{ print $4 }'` : 使用双引号作为分隔符号

