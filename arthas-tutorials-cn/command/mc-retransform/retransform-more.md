> 加载外部的`.class`文件，retransform jvm 已加载的类。

参考：[Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)

### 查看 retransform entry

`retransform -l`{{execute T2}}

- TransformCount 统计在 ClassFileTransformer#transform 函数里尝试返回 entry 对应的 .class 文件的次数，但并不表明 transform 一定成功。

### 删除指定 retransform entry

需要指定 id

`retransform -d 1`{{execute T2}}

### 删除所有 retransform entry

`retransform --deleteAll`{{execute T2}}

### 显式触发 retransform

`retransform --classPattern com.example.demo.arthas.user.UserController`{{execute T2}}

> 注意：对于同一个类，当存在多个 retransform entry 时，如果显式触发 retransform，则最后添加的 entry 生效 (id 最大的)。

### 消除 retransform 的影响

如果对某个类执行 retransform 之后，想消除影响，则需要：

- 删除这个类对应的 retransform entry
- 重新触发 retransform

> 如果不清除掉所有的 retransform entry，并重新触发 retransform，则 arthas stop 时，retransform 过的类仍然生效。

在上面删掉 retransform entry，再显式触发 retransform 之后，可以用 `jad`命令来确认之前 retransform 的结果已经被消除了。

再次访问 [/user/0]({{TRAFFIC_HOST1_80}}/user/0)，会抛出异常。
