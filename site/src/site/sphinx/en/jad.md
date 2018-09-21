jad
===

De-compile specified loaded classes.

`jad` helps to *de-compile* the byte code in JVM to the source code to assist you to better understand the logic behind.

F.Y.I
* the de-compiled code will be grammatically highlighted for readability in Arthas Console;
* you have to understand there might be some trivial grammar errors but it won't affect the logic understanding.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|`[c:]`|hashcode of the class loader that loaded the class|
|[E]|turn on regx matching while the default is wildcards matching|

### Usage

When several class loaders loaded the same class:
1. `jad` to get the hashcode of the class loader;
2. `jad -c <hashcode>` to get the de-compiled class loaded by the class loader.

```java
$ jad org.apache.log4j.Logger

 Found more than one class for: org.apache.log4j.Logger, Please use jad -c hashcode org.apache.log4j.Logger
 HASHCODE  CLASSLOADER
 69dcaba4  +-monitor's ModuleClassLoader
 6e51ad67  +-java.net.URLClassLoader@6e51ad67
             +-sun.misc.Launcher$AppClassLoader@6951a712
               +-sun.misc.Launcher$ExtClassLoader@6fafc4c2
 2bdd9114  +-pandora-qos-service's ModuleClassLoader
 4c0df5f8  +-pandora-framework's ModuleClassLoader

Affect(row-cnt:0) cost in 38 ms.
$ jad org.apache.log4j.Logger -c 69dcaba4

ClassLoader:
+-monitor's ModuleClassLoader

Location:
/Users/zhuyong/Downloads/taobao-hsf.sar/plugins/monitor/lib/log4j-1.2.14.jar

package org.apache.log4j;

import org.apache.log4j.spi.*;

public class Logger extends Category
{
    private static final String FQCN;

    protected Logger(String name)
    {
        super(name);
    }

    public static Logger getLogger(String name)
    {
        return LogManager.getLogger(name);
    }

    public static Logger getLogger(Class clazz)
    {
        return LogManager.getLogger(clazz.getName());
    }

    public static Logger getRootLogger()
    {
        return LogManager.getRootLogger();
    }

    public static Logger getLogger(String name, LoggerFactory factory)
    {
        return LogManager.getLogger(name, factory);
    }

    public void trace(Object message)
    {
        if (repository.isDisabled(5000))
        {
            return;
        }
        if (Level.TRACE.isGreaterOrEqual(getEffectiveLevel()))
        {
            forcedLog(Logger.FQCN, Level.TRACE, message, null);
        }
    }

    public void trace(Object message, Throwable t)
    {
        if (repository.isDisabled(5000))
        {
            return;
        }
        if (Level.TRACE.isGreaterOrEqual(getEffectiveLevel()))
        {
            forcedLog(Logger.FQCN, Level.TRACE, message, t);
        }
    }

    public boolean isTraceEnabled()
    {
        if (repository.isDisabled(5000))
        {
            return false;
        }
        return Level.TRACE.isGreaterOrEqual(getEffectiveLevel());
    }

    static
    {
        Logger.FQCN = Logger.class.getName();
    }

}

Affect(row-cnt:1) cost in 190 ms.
```

De-compile the specified method:

```sh
$ jad com.taobao.container.web.arthas.rest.MetricsController directMetrics

ClassLoader:
+-com.taobao.pandora.boot.loader.ReLaunchURLClassLoader@1817d444
  +-sun.misc.Launcher$AppClassLoader@14dad5dc
    +-sun.misc.Launcher$ExtClassLoader@a38d7a3

Location:
/Users/zhuyong/middleware/tomcat-web/tomcat-web-web/target/classes/

private Map<String, Object> directMetrics(String ip, String[] metrics) {
    JSONObject obj;
    HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("success", false);
    String metricUrl = "http://" + ip + ":8006/metrics/specific";
    String postBody = Arrays.stream(metrics).map(metric -> "metric=" + metric).collect(Collectors.joining("&"));
    HttpClientUtils.Response resp = HttpClientUtils.sendPostRequest((String)metricUrl, (String)postBody);
    if (resp.isSuccess() && (obj = JSON.parseObject(resp.getContent())).containsKey("success") && obj.getBoolean("success").booleanValue() && obj.containsKey("data")) {
        JSONArray dataArray = obj.getJSONArray("data");
        HashMap<String, Object> metricMap = new HashMap<String, Object>();
        for (Object aDataArray : dataArray) {
            JSONObject o = (JSONObject)aDataArray;
            metricMap.put(o.getString("metric"), o.get("value"));
        }
        result.put("data", metricMap);
        result.put("success", true);
        return result;
    }
    return result;
}

Affect(row-cnt:1) cost in 1508 ms.
```
