
watch的结果值由计算`watch-express` ognl表达式产生，可以通过改变ognl表达式来生成想要的值，请参考[OGNL文档](https://commons.apache.org/proper/commons-ognl/language-guide.html)。

> Maps can also be created using a special syntax.
>
>#{ "foo" : "foo value", "bar" : "bar value" }
>
>This creates a Map initialized with mappings for "foo" and "bar".

下面的命令生成map格式的值：

```bash
watch *MathGame prime* '#{ "params" : params, "returnObj" : returnObj, "throwExp": throwExp}' -x 2 -n 5
```

在Telnet shell/WebConsole 中执行上面的命令，输出的结果：

```bash
ts=2020-08-06 16:57:20; [cost=0.241735ms] result=@LinkedHashMap[
    @String[params]:@Object[][
        @Integer[1],
    ],
    @String[returnObj]:@ArrayList[
        @Integer[2],
        @Integer[241],
        @Integer[379],
    ],
    @String[throwExp]:null,
]
```

用Http api 执行上面的命令，注意对JSON双引号转义：

`curl -Ss -XPOST http://localhost:8563/api -d @- << EOF
{
  "action":"exec",
  "execTimeout": 30000,
  "command":"watch *MathGame prime* '#{ \"params\" : params, \"returnObj\" : returnObj, \"throwExp\": throwExp}' -n 3 "
}
EOF`{{execute T3}}

Http api 执行结果：

```json
{
    "body": {
         ...
        "results": [
            ...
            {
                ...
                "type": "watch",
                "value": {
                    "params": [
                        1
                    ],
                    "returnObj": [
                        2,
                        5,
                        17,
                        23,
                        23
                    ]
                }
            },
            {
                ...
                "type": "watch",
                "value": {
                    "params": [
                        -98278
                    ],
                    "throwExp": {
                        "@type": "java.lang.IllegalArgumentException",
                        "localizedMessage": "number is: -98278, need >= 2",
                        "message": "number is: -98278, need >= 2",
                        "stackTrace": [
                            ...
                        ]
                    }
                }
            },
            ...
}
```

可以看到watch结果的value变成map对象，程序可以通过key读取结果。
