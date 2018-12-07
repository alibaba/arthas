redefine
===

> 加载外部的`.class`文件，redefine jvm已加载的类。

参考：[Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> 注意， redefine后的原来的类不能恢复，redefine有可能失败（比如增加了新的field），参考jdk本身的文档。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|[c:]|ClassLoader的hashcode|
|[p:]|外部的`.class`文件的完整路径，支持多个|



### 使用参考

```bash
   redefine -p /tmp/Test.class
   redefine -c 327a647b -p /tmp/Test.class /tmp/Test\$Inner.class
```

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