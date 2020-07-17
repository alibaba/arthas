
Specify customization in `$USER_HOME/.arthas/conf/inputrc` file in the current user home directory.

Vim user may want to map `ctrl+h` to moving the cursor forward one character. To achieve this, copy the default configuration first,

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

then replace `"\C-h": backward-delete-char` with `"\C-h": backward-char`, then reconnect to Arthas console to take effect.
