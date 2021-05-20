redefine
========

> Recommend to use the [retransform](retransform.md) command.

[`mc-redefine` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-mc-redefine)

> Load the external `*.class` files to re-define the loaded classes in JVM.

Reference: [Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

### Frequently asked questions

> Recommend to use the [retransform](retransform.md) command.

* The class of `redefine` cannot modify, add or delete the field and method of the class, including method parameters, method names and return values.

* If `mc` fails, you can compile the class file in the local development environment, upload it to the target system, and use `redefine` to hot load the class.

* At present, `redefine` conflicts with `watch / trace / jad / tt` commands. Reimplementing `redefine` function in the future will solve this problem.

> Notes: Re-defined classes cannot be restored. There are chances that redefining may fail due to some reasons, for example: there's new field introduced in the new version of the class, pls. refer to JDK's documentation for the limitations.

> The `reset` command is not valid for classes that have been processed by `redefine`. If you want to reset, you need `redefine` the original bytecode.


> The `redefine` command will conflict with the `jad`/`watch`/`trace`/`monitor`/`tt` commands. After executing `redefine`, if you execute the above mentioned command, the bytecode of the class will be reset.
> The reason is that in the JDK `redefine` and `retransform` are different mechanisms. When two mechanisms are both used to update the bytecode, only the last modified will take effect.

### Options

|Name|Specification|
|---:|:---|
|`[c:]`|hashcode of the class loader|
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |


### Usage

```bash
redefine /tmp/Test.class
redefine -c 327a647b /tmp/Test.class /tmp/Test\$Inner.class
redefine --classLoaderClass sun.misc.Launcher$AppClassLoader /tmp/Test.class /tmp/Test\$Inner.class
```

### Use with the jad/mc command

```bash
jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java

mc /tmp/UserController.java -d /tmp

redefine /tmp/com/example/demo/arthas/user/UserController.class
```

* Use `jad` command to decompile bytecode, and then you can use other editors, such as vim to modify the source code.
* `mc` command to compile the modified code
* Load new bytecode with `redefine` command

### Tips for uploading .class files to the server

The `mc` command may fail. You can modify the code locally, compile it, and upload it to the server. Some servers do not allow direct uploading files, you can use the `base64` command to bypass.

1. Convert the `.class` file to base64 first, then save it as result.txt

    ```bash
    Base64 < Test.class > result.txt
    ```

2. Login the server, create and edit `result.txt`, copy the local content, paste and save

3. Restore `result.txt` on the server to `.class`

    ```
    Base64 -d < result.txt > Test.class
    ```

4. Use the md5 command to verify that the `.class` files are consistent.


### Restrictions of the redefine command

* New field/method is not allowed
* The function that is running, no exit can not take effect, such as the new `System.out.println` added below, only the `run()` function will take effect.

    ```java
    public class MathGame {
        public static void main(String[] args) throws InterruptedException {
            MathGame game = new MathGame();
            while (true) {
                game.run();
                TimeUnit.SECONDS.sleep(1);
                // This doesn't work because the code keeps running in while
                System.out.println("in loop");
            }
        }

        public void run() throws InterruptedException {
            // This works because the run() function ends completely every time
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