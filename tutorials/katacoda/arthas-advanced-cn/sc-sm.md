
下面介绍Arthas里查找已加载类的命令。

### sc

`sc` 命令可以查找到所有JVM已经加载到的类。


如果搜索的是接口，还会搜索所有的实现类。比如查看所有的`Filter`实现类：

`sc javax.servlet.Filter`{{execute T2}}

通过`-d`参数，可以打印出类加载的具体信息，很方便查找类加载问题。

`sc -d javax.servlet.Filter`{{execute T2}}

`sc`支持通配，比如搜索所有的`StringUtils`：

`sc *StringUtils`{{execute T2}}

### sm

`sm`命令则是查找类的具体函数。比如：

`sm java.math.RoundingMode`{{execute T2}}

通过`-d`参数可以打印函数的具体属性：

`sm -d java.math.RoundingMode`{{execute T2}}

也可以查找特定的函数，比如查找构造函数：

`sm java.math.RoundingMode <init>`{{execute T2}}

