
|  快捷键   | 快捷键说明  |  命令名称  |  命令说明  |
|  ----  | ----  |  ----  | ----  |
| `"\C-a"`  | ctrl + a | beginning-of-line  | 跳到行首 |
| `"\C-e"`  | ctrl + e | end-of-line  | 跳到行尾 |
| `"\C-f"`  | ctrl + f | forward-word  | 向前移动一个单词 |
| `"\C-b"`  | ctrl + b | backward-word  | 向后移动一个单词 |
| `"\e[D"`  | 键盘左方向键 | backward-char  | 光标向前移动一个字符 |
| `"\e[C"`  | 键盘右方向键 | forward-char  | 光标向后移动一个字符 |
| `"\e[B"`  | 键盘下方向键 | next-history  | 下翻显示下一个命令 |
| `"\e[A"`  | 键盘上方向键 | previous-history  | 上翻显示上一个命令 |		
| `"\C-h"`  | ctrl + h | backward-delete-char  | 向后删除一个字符 |
| `"\C-?"`  | ctrl + shift + / | backward-delete-char  | 向后删除一个字符 |
| `"\C-u"`  | ctrl + u | undo  | 撤销上一个命令，相当于清空当前行 |
| `"\C-d"`  | ctrl + d | delete-char  | 删除当前光标到行尾的所有字符 |
| `"\C-k"`  | ctrl + k | kill-line  | 光标向前移动一个字符 |
| `"\C-i"`  | ctrl + i | complete  | 自动补全，相当于敲`TAB` |
| `"\C-j"`  | ctrl + j | accept-line  | 结束当前行，相当于敲回车 |
| `"\C-m"`  | ctrl + m | accept-line  | 结束当前行，相当于敲回车 |				
| `"\C-w"`  |  | backward-delete-word  |  |
| `"\C-x\e[3~"`  |  | backward-kill-line  |  |
| `"\e\C-?"`  |  | backward-kill-word  |  |			

任何时候使用 `tab`键，会根据当前的输入给出提示

命令后敲 `-` 或 `--` ，然后按 `tab` 键，可以展示出此命令具体的选项
