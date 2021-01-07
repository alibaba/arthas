base64
===


> base64编码转换，和linux里的 base64 命令类似。


### 对文件进行 base64 编码

```bash
[arthas@70070]$ echo 'abc' > /tmp/test.txt
[arthas@70070]$ cat /tmp/test.txt
abc

[arthas@70070]$ base64 /tmp/test.txt
YWJjCg==
```

### 对文件进行 base64 编码并把结果保存到文件里

```bash
$ base64 --input /tmp/test.txt --output /tmp/result.txt
```

### 用 base64 解码文件

```
$ base64 -d /tmp/result.txt
abc
```

### 用 base64 解码文件并保存结果到文件里

```bash
$ base64 -d /tmp/result.txt --output /tmp/bbb.txt
```
