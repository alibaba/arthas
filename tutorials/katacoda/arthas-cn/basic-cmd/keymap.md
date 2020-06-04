
通过`kaymap`命令可以输出当前的快捷键映射表。

`keymap`{{execute T2}}

## 默认的快捷键

![SHortcut Key](/hollowman/scenarios/arthas-cn/assets/shortcutkey.png)

任何时候使用 `tab`{{execute T2}}键，会根据当前的输入给出提示

命令后敲 `-` 或 `--` ，然后按 `tab` 键，可以展示出此命令具体的选项

## 自定义快捷键

在当前用户目录下新建`$USER_HOME/.arthas/conf/inputrc`文件，可加入自定义配置。

假设我是vim的重度用户，我要把`ctrl+h`设置为光标向前一个字符，则设置如下，首先拷贝默认配置

```
"\C-a": beginning-of-line
"\C-e": end-of-line
"\C-f": forward-word
"\C-b": backward-word
"\e[D": backward-char
"\e[C": forward-char
"\e[B": next-history
"\e[A": previous-history
"\C-h": backward-delete-char
"\C-?": backward-delete-char
"\C-u": undo
"\C-d": delete-char
"\C-k": kill-line
"\C-i": complete
"\C-j": accept-line
"\C-m": accept-line
"\C-w": backward-delete-word
"\C-x\e[3~": backward-kill-line
"\e\C-?": backward-kill-word
```

然后把`"\C-h": backward-delete-char`换成`"\C-h": backward-char`，然后重新连接即可。

## 后台异步命令相关快捷键

ctrl + c: 终止当前命令

ctrl + z: 挂起当前命令，后续可以 bg/fg 重新支持此命令，或 kill 掉

ctrl + a: 回到行首

ctrl + e: 回到行尾

