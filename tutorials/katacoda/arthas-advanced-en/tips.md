
Some tips on using Arthas.

### help

Every command in Arthas has help doc. Can be viewed with `-h`. There are `EXAMPLES` and `WIKI` links in the help doc.

such as:

`sysprop -h`{{execute T2}}

### Auto completion

Arthas supports a wide range of auto-completion features, and you can type `Tab` to get more information when you have doubts about your use.

For example, after typing `sysprop java.`, enter `Tab`, which will complete the corresponding key:

```
$ sysprop java.
java.runtime.name             java.protocol.handler.pkgs    java.vm.version
java.vm.vendor                java.vendor.url               java.vm.name
...
```


### Readline shortcut key support

Arthas supports common command line shortcuts, such as `Ctrl + A` to jump to the beginning of the line, and `Ctrl + E` to jump to the end of the line.

More shortcuts can be viewed with the `keymap`{{execute T2}} command.


### Completion of history commands

If you want to execute the previous command again, you can match the previous command by pressing `Up/↑` or `Ddown/↓` when you enter halfway.

For example, if `sysprop java.version` was executed before, then after entering `sysprop ja`, you can type `Up/↑`, and it will be automatically completed as `sysprop java.version`.

If you want to see all the history commands, you can also view them with the `history`{{execute T2}} command.

### pipeline

Arthas supports some simple commands after the pipeline, such as:

`sysprop | grep java`{{execute T2}} 

`sysprop | wc -l`{{execute T2}} 