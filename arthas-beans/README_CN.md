##step1 在您的pom.xml中增加profile配置
```
<profiles>
    <!-- macos -->
    <profile>
        <id>macos</id>
        <activation>
            <os>
                <family>mac</family>
            </os>
        </activation>
        <properties>
            <os_family>macos</os_family>
        </properties>
    </profile>

    <!-- linux -->
    <profile>
        <id>linux</id>
        <activation>
            <os>
                <name>linux</name>
            </os>
        </activation>
        <properties>
            <os_family>linux</os_family>
        </properties>
    </profile>
    
    <!-- windows -->
    <profile>
        <id>windows</id>
        <activation>
            <os>
                <family>windows</family>
            </os>
        </activation>
        <properties>
            <os_family>windows</os_family>
        </properties>
    </profile>
</profiles>
```
##step2 在您的pom.xml中引入依赖
PS：一定要配置好profile，不然无法自动生成`<classifier>`配置。
```
<dependency>
    <groupId>com.taobao.arthas</groupId>
    <artifactId>mana-pool-analyzer</artifactId>
    <version>${arthas.vision}</version>
    <classifier>${os_family}-${os.arch}</classifier>
</dependency>
```

##step3 如果没有合适的版本
1. 如果是32位的操作系统，不好意思，目前暂不支持。

2. 如果是64位操作系统，请先尝试`mvn clean compile package install -DskipTests=true -U`把依赖安装到本地；

3. 如果报错，请先确保您本地的g++编译器能够正常工作，再重新尝试`mvn clean compile package install -DskipTests=true -U`；

如有问题请联系`1936978077@qq.com`。
