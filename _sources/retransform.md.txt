retransform
===

[`mc-retransform`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-mc-retransform)

> 加载外部的`.class`文件，retransform jvm已加载的类。

参考：[Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)


### 使用参考

```bash
   retransform /tmp/Test.class
   retransform -l
   retransform -d 1                    # delete retransform entry
   retransform --deleteAll             # delete all retransform entries
   retransform --classPattern demo.*   # triger retransform classes
   retransform -c 327a647b /tmp/Test.class /tmp/Test\$Inner.class
   retransform --classLoaderClass 'sun.misc.Launcher$AppClassLoader' /tmp/Test.class
```

### retransform 指定的 .class 文件

```bash
$ retransform /tmp/MathGame.class
retransform success, size: 1, classes:
demo.MathGame
```

加载指定的 .class 文件，然后解析出class name，再retransform jvm中已加载的对应的类。每加载一个 `.class` 文件，则会记录一个 retransform entry.

> 如果多次执行 retransform 加载同一个 class 文件，则会有多条 retransform entry.

### 查看 retransform entry

```bash
$ retransform -l
Id              ClassName       TransformCount  LoaderHash      LoaderClassName
1               demo.MathGame   1               null            null
```

* TransformCount 统计在 ClassFileTransformer#transform 函数里尝试返回 entry对应的 .class文件的次数，但并不表明transform一定成功。

### 删除指定 retransform entry

需要指定 id：

```bash
retransform -d 1
```

### 删除所有 retransform entry

```bash
retransform --deleteAll
```

### 显式触发 retransform

```bash
$ retransform --classPattern demo.MathGame
retransform success, size: 1, classes:
demo.MathGame
```

> 注意：对于同一个类，当存在多个 retransform entry时，如果显式触发 retransform ，则最后添加的entry生效(id最大的)。

### 消除 retransform 的影响

如果对某个类执行 retransform 之后，想消除影响，则需要：

* 删除这个类对应的 retransform entry
* 重新触发 retransform

> 如果不清除掉所有的 retransform entry，并重新触发 retransform ，则arthas stop时，retransform过的类仍然生效。


### 结合 jad/mc 命令使用

```bash
jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java

mc /tmp/UserController.java -d /tmp

retransform /tmp/com/example/demo/arthas/user/UserController.class
```

* jad命令反编译，然后可以用其它编译器，比如vim来修改源码
* mc命令来内存编译修改过的代码
* 用retransform命令加载新的字节码

### 上传 .class 文件到服务器的技巧

使用`mc`命令来编译`jad`的反编译的代码有可能失败。可以在本地修改代码，编译好后再上传到服务器上。有的服务器不允许直接上传文件，可以使用`base64`命令来绕过。

1. 在本地先转换`.class`文件为base64，再保存为result.txt

    ```bash
    base64 < Test.class > result.txt
    ```

2. 到服务器上，新建并编辑`result.txt`，复制本地的内容，粘贴再保存

3. 把服务器上的 `result.txt`还原为`.class`

    ```
    base64 -d < result.txt > Test.class
    ```

4. 用md5命令计算哈希值，校验是否一致

### retransform的限制

* 不允许新增加field/method
* 正在跑的函数，没有退出不能生效，比如下面新增加的`System.out.println`，只有`run()`函数里的会生效

    ```java
    public class MathGame {
        public static void main(String[] args) throws InterruptedException {
            MathGame game = new MathGame();
            while (true) {
                game.run();
                TimeUnit.SECONDS.sleep(1);
                // 这个不生效，因为代码一直跑在 while里
                System.out.println("in loop");
            }
        }

        public void run() throws InterruptedException {
            // 这个生效，因为run()函数每次都可以完整结束
            System.out.println("call run()");
            try {
                int number = random.nextInt();
                List<Integer> primeFactors = primeFactors(number);
                print(number, primeFactors);

            } catch (Exception e) {
                System.out.println(String.format("illegalArgumentCount:%3d, ", illegalArgumentCount) + e.getMessage());
            }
        }
```