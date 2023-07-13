
> Load the external `*.class` files to retransform the loaded classes in JVM.

Reference: [Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)


### View retransform entry

`retransform -l`{{execute T2}}

```bash
$ retransform -l
Id              ClassName       TransformCount  LoaderHash      LoaderClassName
1               com.example.dem 1               null            null
                o.arthas.user.U
                serController
```

* TransformCount counts the times of attempts to return the .class file corresponding to the entry in the ClassFileTransformer#transform method, but it does not mean that the transform must be successful.

### Delete the specified retransform entry

`retransform -d 1`{{execute T2}}

Need to specify id:

```bash
retransform -d 1
```

### Delete all retransform entries

`retransform --deleteAll`{{execute T2}}

```bash
retransform --deleteAll
```

### Explicitly trigger retransform

`retransform --classPattern com.example.demo.arthas.user.UserController`{{execute T2}}

```bash
$ retransform --classPattern com.example.demo.arthas.user.UserController
retransform success, size: 1, classes:
com.example.demo.arthas.user.UserController
```

> Note: For the same class, when there are multiple retransform entries, if retransform is explicitly triggered, the entry added last will take effect (the one with the largest id).

### Eliminate the influence of retransform

If you want to eliminate the impact after performing retransform on a class, you need to:

* Delete the retransform entry corresponding to this class
* Re-trigger retransform

> If you do not clear all retransform entries and trigger retransform again, the retransformed classes will still take effect when arthas stop.


After deleting the retransform entry above and explicitly triggering the retransform, you can use the `jad` command to confirm that the result of the previous retransform has been eliminated.

Visit https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/user/0 again, an exception will be thrown.

