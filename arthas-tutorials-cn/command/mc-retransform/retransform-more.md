
> 加载外部的`.class`文件，retransform jvm已加载的类。

参考：[Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)


### 查看 retransform entry

`retransform -l`{{execute T2}}

```bash
$ retransform -l
Id              ClassName       TransformCount  LoaderHash      LoaderClassName
1               com.example.dem 1               null            null
                o.arthas.user.U
                serController
```

* TransformCount 统计在 ClassFileTransformer#transform 函数里尝试返回 entry对应的 .class文件的次数，但并不表明transform一定成功。

### 删除指定 retransform entry

`retransform -d 1`{{execute T2}}

需要指定 id：

```bash
retransform -d 1
```

### 删除所有 retransform entry

`retransform --deleteAll`{{execute T2}}

```bash
retransform --deleteAll
```

### 显式触发 retransform

`retransform --classPattern com.example.demo.arthas.user.UserController`{{execute T2}}

```bash
$ retransform --classPattern com.example.demo.arthas.user.UserController
retransform success, size: 1, classes:
com.example.demo.arthas.user.UserController
```

> 注意：对于同一个类，当存在多个 retransform entry时，如果显式触发 retransform ，则最后添加的entry生效(id最大的)。

### 消除 retransform 的影响

如果对某个类执行 retransform 之后，想消除影响，则需要：

* 删除这个类对应的 retransform entry
* 重新触发 retransform

> 如果不清除掉所有的 retransform entry，并重新触发 retransform ，则arthas stop时，retransform过的类仍然生效。

在上面删掉 retransform entry，再显式触发 retransform之后，可以用 `jad`命令来确认之前retransform的结果已经被消除了。

再次访问 https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/0 ，会抛出异常。
