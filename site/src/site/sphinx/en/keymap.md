Arthas Console Keymap
===

`keymap` command print current keymap.

The default keymap is:


| Shortcut      | Shortcut Description   | Command Name   | Command Description  |
| ------------- | ---------------- | -------------------- | ---------------- |
| `"\C-a"`      | ctrl + a         | beginning-of-line    | go to the beginning of the line |
| ` "\C-e"`     | ctrl + e         | end-of-line          | go to the end of the line|
| `"\C-f"`      | ctrl + f         | forward-word         | forward a word         |
| `"\C-b"`      | ctrl + b         | backward-word        | backward a word         |
| `"\e[D"`      | left arrow       | backward-char        | backward a character   |
| `"\e[C"`      | right arrow      | forward-char         | forward a character |
| `"\e[B"`      | down arrow         | next-history       | show next history command        |
| `"\e[A"`      | up arrow       | previous-history     | show previous history command       |
| `"\C-h"`      | ctrl + h         | backward-delete-char | backward delete a character         |
| `"\C-?"`      | ctrl + shift + / | backward-delete-char | backward delete a character        |
| `"\C-u"`      | ctrl + u         | undo                 | clear current line |
| `"\C-d"`      | ctrl + d         | delete-char          | delete the character of the current cursor   |
| `"\C-k"`      | ctrl + k         | kill-line            | delete all characters from the current cursor to the end of the line   |
| `"\C-i"`      | ctrl + i         | complete             | Auto completion, equivalent to `TAB`   |
| `"\C-j"`      | ctrl + j         | accept-line          | end the current line, equivalent to `enter` |
| `"\C-m"`      | ctrl + m         | accept-line          | end the current line, equivalent to `enter`     |
| `"\C-w"`      |                  | backward-delete-word |                  |
| `"\C-x\e[3~"` |                  | backward-kill-line   |                  |
| `"\e\C-?"`    |                  | backward-kill-word   |                  |


* You can enter `Tab` to get automatic prompts at any time.
* After typing the command, type `-` or `--`, then press `tab` to display the specific options of this command.


#### Custom shortcuts

Create a new `$USER_HOME/.arthas/conf/inputrc` file in the current user home directory and add a custom configuration.

Suppose I am a heavy user of vim. I want to set `ctrl+h` to the cursor forward character. Set it as follows, first copy the default configuration.

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

Then replace `"\C-h": backward-delete-char` with `"\C-h": backward-char`, then reconnect.

#### Shortcuts about async

* ctrl + c: Terminate current command
* ctrl + z: Suspend the current command, you can restore this command with bg/fg, or kill it.
* ctrl + a: Go to the beginning the line 
* ctrl + e: Go to the end of the line




