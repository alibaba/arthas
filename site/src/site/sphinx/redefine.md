redefine
===

> 加载外部的`.class`文件，redefine jvm已加载的类。

参考：[Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> 注意， redefine后的原来的类不能恢复，redefine有可能失败（比如增加了新的field），参考jdk本身的文档。

> `reset`命令对`redefine`的类无效。如果想重置，需要`redefine`原始的字节码。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|[c:]|ClassLoader的hashcode|
|[p:]|外部的`.class`文件的完整路径，支持多个|



### 使用参考

```bash
   redefine /tmp/Test.class
   redefine -c 327a647b /tmp/Test.class /tmp/Test\$Inner.class
```

### 结合 jad/mc 命令使用

```bash
jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java

mc /tmp/UserController.java -d /tmp

redefine /tmp/com/example/demo/arthas/user/UserController.class
```

* jad命令反编译，然后可以用其它编译器，比如vim来修改源码
* mc命令来内存编译修改过的代码
* 用redefine命令加载新的字节码

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

### redefine的限制

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