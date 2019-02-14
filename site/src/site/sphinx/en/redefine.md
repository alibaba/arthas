redefine
========

> Load the external `*.class` files to re-define the loaded peer class in JVM.

Reference: [Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> Notes: Re-defined classes cannot be restored. There are chances that redefining may fail due to some reasons, for example: there's new field introduced in the new version of the class, pls. refer to JDK's documentation for the limitations.

### Options

|Name|Specification|
|---:|:---|
|`[c:]`|hashcode of the class loader|
|`[p:]`|absolute path of the external `*.class`, multiple paths are separated with 'space'|


### Usage

```bash
redefine /tmp/Test.class
redefine -c 327a647b /tmp/Test.class /tmp/Test$Inner.class
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