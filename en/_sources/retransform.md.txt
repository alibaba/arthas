retransform
========

[`mc-retransform` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-mc-retransform)

> Load the external `*.class` files to retransform the loaded classes in JVM.

Reference: [Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)

### Usage

```bash
   retransform /tmp/Test.class
   retransform -l
   retransform -d 1                    # delete retransform entry
   retransform --deleteAll             # delete all retransform entries
   retransform --classPattern demo.*   # triger retransform classes
   retransform -c 327a647b /tmp/Test.class /tmp/Test\$Inner.class
   retransform --classLoaderClass 'sun.misc.Launcher$AppClassLoader' /tmp/Test.class
```

### retransform the specified .class file 

```bash
$ retransform /tmp/MathGame.class
retransform success, size: 1, classes:
demo.MathGame
```

Load the specified .class file, then parse out the class name, and then retransform the corresponding class loaded in the jvm. Every time a `.class` file is loaded, a retransform entry is recorded.

> If retransform is executed multiple times to load the same class file, there will be multiple retransform entries.

### View retransform entry

```bash
$ retransform -l
Id              ClassName       TransformCount  LoaderHash      LoaderClassName
1               demo.MathGame   1               null            null
```

* TransformCount counts the times of attempts to return the .class file corresponding to the entry in the ClassFileTransformer#transform method, but it does not mean that the transform must be successful.
### Delete the specified retransform entry

Need to specify id:

```bash
retransform -d 1
```

### Delete all retransform entries

```bash
retransform --deleteAll
```

### Explicitly trigger retransform

```bash
$ retransform --classPattern demo.MathGame
retransform success, size: 1, classes:
demo.MathGame
```

> Note: For the same class, when there are multiple retransform entries, if retransform is explicitly triggered, the entry added last will take effect (the one with the largest id).

### Eliminate the influence of retransform

If you want to eliminate the impact after performing retransform on a class, you need to:

* Delete the retransform entry corresponding to this class
* Re-trigger retransform

> If you do not clear all retransform entries and trigger retransform again, the retransformed classes will still take effect when arthas stop.

### Use with the jad/mc command

```bash
jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java

mc /tmp/UserController.java -d /tmp

retransform /tmp/com/example/demo/arthas/user/UserController.class
```

* Use `jad` command to decompile bytecode, and then you can use other editors, such as vim to modify the source code.
* `mc` command to compile the modified code
* Load new bytecode with `retransform` command

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


### Restrictions of the retransform command

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