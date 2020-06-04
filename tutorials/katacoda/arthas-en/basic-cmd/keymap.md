
`kaymap` can list all Arthas keyboard shortcuts and shortcut customizations.

`keymap`{{execute T2}}

## The default keymap

![SHortcut Key](/hollowman/scenarios/arthas-en/assets/shortcutkey.png)

Press `tab`{{execute T2}} to enable auto-completion prompt at any time.

Enter command and `-` or `--`, then press `tab` to display the concrete options for the current command.

## Custom shortcuts

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

## Shortcuts for jobs

ctrl + c: Terminate current command

ctrl + z: Suspend the current command, you can restore this command with bg/fg, or kill it.

ctrl + a: Go to the beginning the line

ctrl + e: Go to the end of the line

