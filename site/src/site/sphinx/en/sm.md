sm
==

> Search method from the loaded classes.

`sm` stands for search method. This command can search and show method information from all loaded classes. `sm` can only view the methods declared on the target class, that is, methods from its parent classes are invisible.


### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for class name|
|*method-pattern*|pattern for method name|
|`[d]`|print the details of the method|
|`[E]`|turn on regex matching while the default mode is wildcard matching|

### Usage

Show methods from `org.apache.catalina.connector.Connector`:

```bash
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

Show methods' details from `org.apache.catalina.connector.Connector`:

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
