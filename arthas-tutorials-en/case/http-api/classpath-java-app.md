
Get system properties of the Java application through Http api and
extract the value of `java.class.path`.

`json_data=$(curl -Ss -XPOST http://localhost:8563/api -d '
{
  "action":"exec",
  "command":"sysprop"
}')`{{execute T3}}

* Extract value with `sed`:

`class_path=$(echo $json_data | tr -d '\n' | sed 's/.*"java.class.path":"\([^"]*\).*/\1/')
echo "classpath: $class_path"`{{execute T3}}

* Extract value with `json_pp/awk`:

`class_path=$(echo $json_data | tr -d '\n' | json_pp | grep java.class.path | awk -F'"' '{ print $4 }')
echo "classpath: $class_path"`{{execute T3}}

Output:

```
classpath: math-game.jar
```

NOTE:

* `echo $json_data | tr -d '\n'` : Delete line breaks (the value of
  `line.separator`) to avoid affecting the processing of `sed`/`json_pp`
  commands.
* `awk -F'"' '{ print $4 }'` : Use double quote as delimiter
