Specify customization in `$USER_HOME/.arthas/conf/inputrc` file in the current user home directory.

`exit`{{execute interrupt}}

`mkdir -p /root/.arthas/conf/`{{execute T2}}

The default configuration is as follows:

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

Vim user may want to map `ctrl+h` to moving the cursor forward one character. To achieve this, replace `"\C-h": backward-delete-char` with `"\C-h": backward-char` in the default configuration. The modified configuration is as follows:

`echo '\C-a": beginning-of-line\n"\C-e": end-of-line\n"\C-f": forward-word\n"\C-b": backward-word\n"\e[D": backward-char\n"\e[C": forward-char\n"\e[B": next-history\n"\e[A": previous-history\n"\C-h": backward-char\n"\C-?": backward-delete-char\n"\C-u": undo\n"\C-d": delete-char\n"\C-k": kill-line\n"\C-i": complete\n"\C-j": accept-line"\C-m": accept-line\n"\C-w": backward-delete-word\n"\C-x\e[3~": backward-kill-line\n"\e\C-?": backward-kill-word'`{{execute T2}}

Then reconnect to Arthas console to take effect.

`java -jar arthas-boot.jar`{{execute interrupt}}

`1`{{execute T2}}

Finally you can see the modification by executing

`keymap | grep C-h`{{execute T2}}
