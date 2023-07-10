
通过`history`命令可以打印命令历史。

`history`{{execute T2}}

## 使用参考

`history -h`{{execute T2}}

```bash
[arthas@48]$ history -h
 USAGE:
   history [-c] [-h] [n]

 SUMMARY:
   Display command history

 EXAMPLES:
   history
   history -c
   history 5

 OPTIONS:
 -c, --clear                          clear history
 -h, --help                           this help
 <n>                                  how many history commnads to display
```

## 显示指定数目的历史

`history 5`{{execute T2}}

## 清除历史

`history -c`{{execute T2}}
