
首先退出Arthas, 在当前用户目录下新建`$USER_HOME/.arthas/conf/inputrc`文件，可加入自定义配置。

`exit`{{execute interrupt}}

`mkdir -p /root/.arthas/conf/`{{execute T2}}

假设我是vim的重度用户，我要把`ctrl+h`设置为光标向前一个字符，则首先拷贝默认配置:

```text
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

然后把`"\C-h": backward-delete-char`换成`"\C-h": backward-char`，修改后的keymap配置即以下内容:

`cat > /root/.arthas/conf/inputrc <<EOF
"\C-a": beginning-of-line
"\C-e": end-of-line
"\C-f": forward-word
"\C-b": backward-word
"\e[D": backward-char
"\e[C": forward-char
"\e[B": next-history
"\e[A": previous-history
"\C-h": backward-char
"\C-?": backward-delete-char
"\C-u": undo
"\C-d": delete-char
"\C-k": kill-line
"\C-i": complete
"\C-j": accept-line
"\C-m": accept-line
"\C-w": backward-delete-word
"\C-x\e[3~": backward-kill-line
"\e\C-?": backward-kill-word`{{execute T2}}

然后重新连接即可。

`java -jar arthas-boot.jar`{{execute interrupt}}

`1`{{execute T2}}

最后执行下面命令查看效果：

`keymap | grep C-h`{{execute T2}}