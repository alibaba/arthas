### Outputting User ID when the ID is greater than 10

`watch com.example.demo.arthas.user.UserController  * "{params[0]}" "params[0] > 10"`{{exec}}

After running the above command, accessing [/user/12]({{TRAFFIC_HOST1_80}}/user/12) will print the user’s ID on the terminal.  
If you access [/user/9]({{TRAFFIC_HOST1_80}}/user/9), there will be no output.

The user can exit the watch command by typing `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}}.

### Calling a method of an object in the method parameter of a class

To modify the value of `name`{{}} by calling the `addAttribute`{{}} method on the `model`{{}} parameter in the `helloWorld`{{}} method of the `com.example.demo.arthas.WelcomeController`{{}} class:
First, access [/hello]({{TRAFFIC_HOST1_80}}/hello) and you will see that the page displays `Hello World - jsp`{{}} .

`watch com.example.demo.arthas.WelcomeController helloWorld "{params[0].addAttribute('name', 'HTML')}"`{{exec}}

After running the above command, access [/hello]({{TRAFFIC_HOST1_80}}/hello) again, and you will see that the page content has changed to `Hello World - HTML`{{}} .

The user can exit the watch command by typing `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}}.

### Outputting the params, target, returnObj and throwExp when an error occurs

`watch com.example.demo.arthas.user.UserController  * "{params, target, returnObj, throwExp}" "throwExp != null"`{{exec}}

After running the above command, accessing [/user/0]({{TRAFFIC_HOST1_80}}/user/0) will output the params, target, returnObj and throwExp.

The user can exit the watch command by typing `Q`{{exec interrupt}} or `Ctrl+C`{{exec interrupt}}.

### Inner class case

First use the command ` sc  '*＄*'  | grep java.lang.Integer`{{exec}} to find the inner class

then use the command `ognl -x 3 '@java.lang.Integer$IntegerCache@low'`{{exec}} to see a specific inner class.

### Note

Please refer to related issues[#71](https://github.com/alibaba/arthas/issues/71) for more useful and interesting examples.
