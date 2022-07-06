history
===

view command history.

> history of commands will persisted in a file named history, so the history command can show all the history commands of current Arthas server ,but not only history in current session.

### Options

| Name | Specification                  |
| ---: | :----------------------------- |
| [c:] | clear all the history commands |
| [n:] | view the nearest 5 commands    |

### 使用参考

```bash
#view the nearest 3 commands
$ history 3
  269  thread
  270  cls
  271  history 3
```

```bash
 #clear all the history commands
 $ history -c
 $ history 3
  1  history 3
```

