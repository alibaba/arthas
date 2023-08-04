watch 的结果值由计算`watch-express` ognl 表达式产生，可以通过改变 ognl 表达式来生成想要的值，请参考[OGNL 文档](https://commons.apache.org/proper/commons-ognl/language-guide.html)。

> Maps can also be created using a special syntax.
>
> #{ "foo" : "foo value", "bar" : "bar value" }
>
> This creates a Map initialized with mappings for "foo" and "bar".

下面的命令生成 map 格式的值：

`watch *MathGame prime* '#{ "params" : params, "returnObj" : returnObj, "throwExp": throwExp}' -x 2 -n 5`{{exec}}  
在 Telnet shell/WebConsole 中执行上面的命令，会输出 `demo.MathGame.primeFactors` 的相关信息。
使用 `Q`{{exec}} 或 `Ctrl+C` 退出 `watch`

用 Http api 执行上面的命令，注意对 JSON 双引号转义：

`curl -Ss -XPOST http://localhost:8563/api -d '{"action":"exec","execTimeout":30000,"command":"watch *MathGame prime* #{\"params\":params,\"returnObj\":returnObj,\"throwExp\":throwExp} -n 3 "}' | json_pp`{{exec}}

Http api 执行，可以看到 watch 结果的 value 变成 map 对象，程序可以通过 key 读取结果。
