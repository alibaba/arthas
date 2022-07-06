Arthas 命令行快捷键
===

[`keymap`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-keymap)

`keymap`命令输出当前的快捷键映射表：

默认的快捷键如下：





| 快捷键           | 快捷键说明            | 命令名称                 | 命令说明             |
| ------------- | ---------------- | -------------------- | ---------------- |
| `"\C-a"`      | ctrl + a         | beginning-of-line    | 跳到行首             |
| ` "\C-e"`     | ctrl + e         | end-of-line          | 跳到行尾             |
| `"\C-f"`      | ctrl + f         | forward-word         | 向前移动一个单词         |
| `"\C-b"`      | ctrl + b         | backward-word        | 向后移动一个单词         |
| `"\e[D"`      | 键盘左方向键           | backward-char        | 光标向前移动一个字符       |
| `"\e[C"`      | 键盘右方向键           | forward-char         | 光标向后移动一个字符       |
| `"\e[B"`      | 键盘下方向键           | next-history         | 下翻显示下一个命令        |
| `"\e[A"`      | 键盘上方向键           | previous-history     | 上翻显示上一个命令        |
| `"\C-h"`      | ctrl + h         | backward-delete-char | 向后删除一个字符         |
| `"\C-?"`      | ctrl + shift + / | backward-delete-char | 向后删除一个字符         |
| `"\C-u"`      | ctrl + u         | undo                 | 撤销上一个命令，相当于清空当前行 |
| `"\C-d"`      | ctrl + d         | delete-char          | 删除当前光标所在字符       |
| `"\C-k"`      | ctrl + k         | kill-line            | 删除当前光标到行尾的所有字符   |
| `"\C-i"`      | ctrl + i         | complete             | 自动补全，相当于敲`TAB`   |
| `"\C-j"`      | ctrl + j         | accept-line          | 结束当前行，相当于敲回车     |
| `"\C-m"`      | ctrl + m         | accept-line          | 结束当前行，相当于敲回车     |
| `"\C-w"`      |                  | backward-delete-word |                  |
| `"\C-x\e[3~"` |                  | backward-kill-line   |                  |
| `"\e\C-?"`    |                  | backward-kill-word   |                  |


* 任何时候 `tab` 键，会根据当前的输入给出提示
* 命令后敲 `-` 或 `--` ，然后按 `tab` 键，可以展示出此命令具体的选项


#### 自定义快捷键

在当前用户目录下新建`$USER_HOME/.arthas/conf/inputrc`文件，加入自定义配置。

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

#### 后台异步命令相关快捷键

* ctrl + c: 终止当前命令
* ctrl + z: 挂起当前命令，后续可以 bg/fg 重新支持此命令，或 kill 掉
* ctrl + a: 回到行首
* ctrl + e: 回到行尾




