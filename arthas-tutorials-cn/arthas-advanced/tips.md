
为了更好使用Arthas，下面先介绍Arthas里的一些使用技巧。

### help

Arthas里每一个命令都有详细的帮助信息。可以用`-h`来查看。帮助信息里有`EXAMPLES`和`WIKI`链接。

比如：

`sysprop -h`{{execute T2}}

### 自动补全

Arthas支持丰富的自动补全功能，在使用有疑惑时，可以输入`Tab`来获取更多信息。

比如输入 `sysprop java.` 之后，再输入`Tab`，会补全出对应的key：

```
$ sysprop java.
java.runtime.name             java.protocol.handler.pkgs    java.vm.version
java.vm.vendor                java.vendor.url               java.vm.name
...
```


### readline的快捷键支持

Arthas支持常见的命令行快捷键，比如`Ctrl + A`跳转行首，`Ctrl + E`跳转行尾。

更多的快捷键可以用 `keymap`{{execute T2}} 命令查看。

### 历史命令的补全

如果想再执行之前的命令，可以在输入一半时，按`Up/↑` 或者 `Ddown/↓`，来匹配到之前的命令。

比如之前执行过`sysprop java.version`，那么在输入`sysprop ja`之后，可以输入`Up/↑`，就会自动补全为`sysprop java.version`。

如果想查看所有的历史命令，也可以通过 `history`{{execute T2}} 命令查看到。


### pipeline

Arthas支持在pipeline之后，执行一些简单的命令，比如：

`sysprop | grep java`{{execute T2}} 

`sysprop | wc -l`{{execute T2}} 