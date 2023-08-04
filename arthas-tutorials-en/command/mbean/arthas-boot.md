Open a new tab, and then in the terminal of `Tab 2`, download `arthas-boot.jar` and start with the `java -jar` command:

`wget https://arthas.aliyun.com/arthas-boot.jar java -jar arthas-boot.jar`{{execute T2}}

`arthas-boot` is the launcher for `Arthas`. It lists all the Java processes, and the user can select the target process to be diagnosed.

Select the first process, type `1`{{execute T2}} ，then type `Enter`：

After the Attach is successful, Arthas LOGO is printed. Enter `help`{{execute T2}} for more help.

![Arthas Boot](../../assets/arthas-boot.png)
