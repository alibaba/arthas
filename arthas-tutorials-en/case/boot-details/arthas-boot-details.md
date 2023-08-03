Open a new tab, and then in the terminal of `Tab 2`, download `arthas-boot.jar` and start with the `java -jar` command:

`wget https://arthas.aliyun.com/arthas-boot.jar`{{execute T2}}

`arthas-boot` is the launcher for `Arthas`. It lists all the Java processes, and the user can select the target process to be diagnosed.

`arthas-boot.jar` supports many parameters and can be viewed by `java -jar arthas-boot.jar -h`{{execute T2}}.
