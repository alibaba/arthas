sm
===

> 查看已加载类的方法信息

“Search-Method” 的简写，这个命令能搜索出所有已经加载了 Class 信息的方法信息。

`sm` 命令只能看到由当前类所声明 (declaring) 的方法，父类则无法看到。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|[d]|展示每个方法的详细信息|
|[E]|开启正则表达式匹配，默认为通配符匹配|

### 使用参考

```
$ sm org.apache.catalina.connector.Connector
org.apache.catalina.connector.Connector-><init>
org.apache.catalina.connector.Connector->setProperty
org.apache.catalina.connector.Connector->getProperty
org.apache.catalina.connector.Connector->toString
org.apache.catalina.connector.Connector->resume
org.apache.catalina.connector.Connector->getScheme
org.apache.catalina.connector.Connector->getProtocol
org.apache.catalina.connector.Connector->getPort
org.apache.catalina.connector.Connector->setService
org.apache.catalina.connector.Connector->setPort
org.apache.catalina.connector.Connector->getService
org.apache.catalina.connector.Connector->getAttribute
org.apache.catalina.connector.Connector->setAttribute
org.apache.catalina.connector.Connector->getLocalPort
org.apache.catalina.connector.Connector->pause
org.apache.catalina.connector.Connector->setProtocol
org.apache.catalina.connector.Connector->initInternal
org.apache.catalina.connector.Connector->setSecure
org.apache.catalina.connector.Connector->getSecure
org.apache.catalina.connector.Connector->startInternal
org.apache.catalina.connector.Connector->stopInternal
org.apache.catalina.connector.Connector->setScheme
org.apache.catalina.connector.Connector->createRequest
org.apache.catalina.connector.Connector->getDomainInternal
org.apache.catalina.connector.Connector->getProtocolHandler
org.apache.catalina.connector.Connector->setURIEncoding
org.apache.catalina.connector.Connector->findSslHostConfigs
org.apache.catalina.connector.Connector->destroyInternal
org.apache.catalina.connector.Connector->getObjectNameKeyProperties
org.apache.catalina.connector.Connector->getAllowTrace
org.apache.catalina.connector.Connector->setAllowTrace
org.apache.catalina.connector.Connector->getAsyncTimeout
org.apache.catalina.connector.Connector->setAsyncTimeout
org.apache.catalina.connector.Connector->getEnableLookups
org.apache.catalina.connector.Connector->setEnableLookups
org.apache.catalina.connector.Connector->getMaxCookieCount
...

```

```bash
$ sm org.apache.catalina.connector.Connector -d
 declaring-class   org.apache.catalina.connector.Connector
 constructor-name  <init>
 modifier          public
 annotation
 parameters
 exceptions

 declaring-class   org.apache.catalina.connector.Connector
 constructor-name  <init>
 modifier          public
 annotation
 parameters        java.lang.String
 exceptions

 declaring-class  org.apache.catalina.connector.Connector
 method-name      setProperty
 modifier         public
 annotation
 parameters       java.lang.String
                  java.lang.String
 return           boolean
 exceptions
 ......
```