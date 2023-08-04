The user can decompile the code with the [jad command](https://arthas.aliyun.com/en/doc/jad.html):

`jad com.example.demo.arthas.user.UserController`{{execute T2}}

The `--source-only` option can only print out the source code:

`jad --source-only com.example.demo.arthas.user.UserController`{{execute T2}}
