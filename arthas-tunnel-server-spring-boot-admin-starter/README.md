## How it works

This starter based on Tunnel server/client use websocket protocol.

For example:

1. Add `arthas-tunnel-server-spring-boot-admin-starter` dependency in your `Spring Boot Admin` service.
    > Notice: the version of `arthas-tunnel-server-spring-boot-admin-starter` and `arthas-spring-boot-starter` must be 3.5.1 or higher.
   
   dependency:
    ```xml
    <dependency>
        <groupId>com.taobao.arthas</groupId>
        <artifactId>arthas-tunnel-server-spring-boot-admin-starter</artifactId>
        <version>3.5.1-SNAPSHOT</version>
    </dependency>
    ```

2. Add `arthas-spring-boot-starter` dependency in your `Spring Boot` service and add tunnel-server url in your config file.
   > Notice: the version of `arthas-tunnel-server-spring-boot-admin-starter` and `arthas-spring-boot-starter` must be 3.5.1 or higher.

   dependency:
   ```xml
   <dependency>
   <groupId>com.taobao.arthas</groupId>
   <artifactId>arthas-tunnel-server-spring-boot-admin-starter</artifactId>
   <version>3.5.1-SNAPSHOT</version>
   </dependency>
    ```
   
    config:
    ```properties
    tunnel-server=ws://{SpringBootAdminServer}:7777/ws
    ```

3. Access the web of springboot admin,and you will see `Arthas` in the nav,click it.

4. Change `Service` and `Instance` in  `Arthas` navbar,then click `Connect` button.
