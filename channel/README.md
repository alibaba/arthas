## Arthas channel server 使用文档

### 运行channel server demo

1. Build Arthas
```shell
cd /path/arthas
mvn clean package -DskipTests
```

2. 启动channel-server-app

```shell
cd /path/arthas/channel/channel-server-app/target
java -jar arthas-channel-server-app-3.5.3.jar
```

3. 启动Arthas并连接到Channel Server

以javaagent方式启动arthas, 并连接到指定channel server地址:

```shell
java -javaagent:/path/arthas-bin/arthas-agent.jar=";telnetPort=-1;httpPort=-1;channelServer=localhost:7700" -jar math-game.jar
```

连接channel server且同时打开telnet/http端口:
```shell
java -javaagent:/path/arthas-bin/arthas-bin/arthas-agent.jar=";channelServer=localhost:7700" -jar math-game.jar
```

有需要可以在agent参数中指定agentId和心跳间隔：
```shell
java -javaagent:/path/arthas-bin/arthas-agent.jar=";telnetPort=-1;httpPort=-1;channelServer=localhost:7700;agentId=myagent_12345;heartbeatInterval=10" -jar math-game.jar
```

>注意：arthas-agent.jar的第一个参数会作为arthas-core.jar的path，如果不指定则自动查找arthas-core.jar, 此时参数需要以';'打头，避免第一个参数被错误解析忽略。

arthas javaagent 参数说明：

| 名称 | 描述 | 默认值 |
| --- | ---- | ----- |
| channelServer | Channel Server 地址 | 无 |
| heartbeatInterval | Arthas 与 Channel Server的心跳间隔时间（秒） | 10（秒） |
| agentId       | Arthas agent id    | 默认从arthas.properties读取，如果不指定则启动时动态生成 |
| telnetPort | Arthas telnet 端口，-1表示禁用 | 3658 |
| httpPort   | Arthas http 端口, -1表示禁用   | 8563 |


### channel server 配置

| 名称 | 描述 | 默认值 |
| --- | ---- | ----- |
| server.port | http端口，处理http api请求及查询agent列表等 | 8800 |
| arthas.channel.server.websocket.enabled | 是否启用WebConsole代理 | true |
| arthas.channel.server.websocket.port    | WebConsole代理端口    | 8801 |
| arthas.channel.server.backend.enabled | 是否启用Backend grpc通道 | true |
| arthas.channel.server.backend.port | Arthas agent连接的Channel server backend端口 | 7700 |


#### 日志配置

使用外部的logger配置:

```shell
java -jar arthas-channel-server-app-3.5.3.jar --logging.config=/path/logback.xml
```

打开Arthas request/response debug log, 修改logback.xml: 
```xml
<logger name="com.alibaba.arthas.channel.server.grpc" level="DEBUG" />
```

logback支持自动加载配置：

```xml
<configuration scan="true" scanPeriod="10 seconds">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="STDOUT" />
    </root>

    <!-- arthas request/response log -->
    <!--<logger name="com.alibaba.arthas.channel.server.grpc" level="DEBUG" />-->

</configuration>
```

### 使用方法

#### 1、 agent列表

http://localhost:8800/agents

#### 2、WebConsole

http://localhost:8800/

输入agentId，点击“Connect”

#### 3、同步执行命令

/agent/{agentId}/exec

```shell
agent_id=becb81b8f33843ca932cd75ff9970318
curl -Ss -XPOST -H "Content-Type: application/json" http://localhost:8800/agent/${agent_id}/exec -d '
{
  "action":"EXECUTE",
  "executeParams": {
  "commandLine": "session",
  "resultFormat": "JSON"
  }
}
'
```

返回结果: 

```json
{
  "agentId": "becb81b8f33843ca932cd75ff9970318",
  "requestId": "LHY9DpPsISDu",
  "status": "SUCCEEDED",
  "sessionId": "9322bad2-33a8-4f7d-bafd-2f7701824bd2",
  "executeResult": {
    "resultsJson": "[{\"agentId\":\"becb81b8f33843ca932cd75ff9970318\",\"channelServer\":\"localhost:7700\",\"javaPid\":9347,\"jobId\":3,\"sessionId\":\"9322bad2-33a8-4f7d-bafd-2f7701824bd2\",\"tunnelConnected\":false,\"type\":\"session\"},{\"jobId\":3,\"statusCode\":0,\"type\":\"status\"}]"
  }
}
```

#### 4、异步执行命令
1) 异步执行命令并返回sse结果
   
   /agent/{agentId}/sse_async_exec

```shell
agent_id=becb81b8f33843ca932cd75ff9970318
curl -Ss -XPOST -H "Content-Type: application/json" http://localhost:8800/agent/${agent_id}/sse_async_exec -d '
{
  "action":"ASYNC_EXECUTE",
  "executeParams": {
  "commandLine": "watch *MathGame prime* -n 1",
  "resultFormat": "JSON"
  }
}'
```

2) 异步执行命令，long-polling 定时拉取结果
   
   /agent/{agentId}/async_exec  
   /agent/{agentId}/results/{requestId}
   
```shell
agent_id=becb81b8f33843ca932cd75ff9970318
curl -Ss -XPOST -H "Content-Type: application/json" http://localhost:8800/agent/${agent_id}/async_exec -d '
{
  "action":"ASYNC_EXECUTE",
  "executeParams": {
  "commandLine": "watch *MathGame prime* -n 10",
  "resultFormat": "JSON"
  }
}'
```

返回结果：

```json
{
  "agentId": "becb81b8f33843ca932cd75ff9970318",
  "requestId": "my7yP6sU7PSe",
  "status": "CONTINUOUS",
  "sessionId": "..."
}
```

提取requestId，循环拉取结果：

```shell
request_id=my7yP6sU7PSe
curl -Ss  -H "Content-Type: application/json" http://localhost:8800/agent/${agent_id}/results/${request_id}
```

#### 5、与Arthas Http API兼容的接口

在ChannelServer 上提供了一个兼容Http API的接口（https://arthas.aliyun.com/doc/http-api.html）
> 注意：  
> 1）需要在url指定agentId 或者在request中指定agentId： /legacy_api/${agent_id}  
> 2）请求参数和响应结果的json格式与Http API接口相同，响应结果中可能缺少部分属性。  
> 3）拉取结果（PULL_RESULTS ）需要指定异步命令的requestId 或 sessionId （建议指定requestId，如果有requestId参数会优先使用它，如果只有sessionId参数则需要一个转换处理）  

```shell
curl -Ss -XPOST -H "Content-Type: application/json" http://localhost:8800/legacy_api/${agent_id} -d '
{
  "action":"exec",
  "command": "version"
}'
```
or

```shell
curl -Ss -XPOST -H "Content-Type: application/json" http://localhost:8800/legacy_api -d '
{
  "action":"exec",
  "command": "version",
  "agentId": "150e6b4018ef4f35bdb9cf0460504b7e"
}'
```

返回结果：

```json
{
  "requestId": "hW66xAPRZpe2",
  "state": "SUCCEEDED",
  "sessionId": "dd71da0d-5afc-456b-87e3-8ed09c1038a4",
  "agentId": "150e6b4018ef4f35bdb9cf0460504b7e",
  "body": {
    "results": [
      {
        "version": "3.5.1",
        "type": "version",
        "jobId": 2
      },
      {
        "jobId": 2,
        "type": "status",
        "statusCode": 0
      }
    ]
  }
}
```