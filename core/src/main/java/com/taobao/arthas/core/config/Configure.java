package com.taobao.arthas.core.config;

import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.util.reflect.ArthasReflectUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;

/**
 * Arthas startup configuration class, used for packaging startup parameters.
 *
 * @author vlinux
 * @author hengyunabc 2018-11-12
 */
@Config(prefix = "arthas")
public class Configure {

    /**
     * the ip of Arthas listening target JVM. 127.0.0.1 by default.
     */
    private String ip;

    /**
     * the port of Arthas listening. 3658 by default.
     */
    private Integer telnetPort;

    /**
     * the port of web console. 8563 by default.
     * Arthas supports the Web Console.
     * After Arthas attach JVM success, the user can access http://127.0.0.1:8563/ to use Arthas on Website.
     */
    private Integer httpPort;

    /**
     *  the pid of Arthas listening target JVM.
     */
    private Long javaPid;

    /**
     * the local file path of arthas-core.jar
     */
    private String arthasCore;

    /**
     * the local file path of arthas-agent.jar
     */
    private String arthasAgent;

    /**
     * the url path of tunnel server.  ex: ws://127.0.0.1:7777/ws .
     *
     * Arthas supports the function of SSH Tunneling that can let user doesn't need to log in remote server and can manage multiple Arthas agent in local.
     * more detail: {@link https://arthas.aliyun.com/doc/tunnel.html#arthas-tunnel-server Arthas Doc}
     */
    private String tunnelServer;

    /**
     * the unified code of Arthas agent.
     * User can specify a specific agentId to connect to a specific agent in Arthas Tunnel.
     * If user specifies the {@link appName}, the agentId will become appName-xxxxx.
     */
    private String agentId;

    /**
     * username for authentication.
     * Arthas supports the function of authentication.
     * Username and password that User can configure it at arthas.properties or specify it when Arthas starting.
     * If the username and password was configured, the user must execute command of auth to verify his identity before everything beginning.
     * more detail: {@link https://arthas.aliyun.com/doc/auth.html Arthas Doc}
     */
    private String username;

    /**
     * password for authentication.
     * If only username is configured and no password is configured, a random password will be generated and printed in ~/logs/arthas/arthas.log .
     */
    private String password;

    /**
     * the log path of Arthas. default by {@link com.taobao.arthas.common.ArthasConstants#ARTHAS_OUTPUT}
     */
    private String outputPath;

    /**
     * the full pathname of the classes that need to be enhanced by the ClassLoader.
     * If more than one is needed, separate by ','.
     */
    private String enhanceLoaders;

    /**
     * the name of attaching application. Just for distinguishing different application.
     * If the user specifies appName when starting Arthas, AppName will be shown when users browse to http://arthas-tunnel-server-ip:port/apps.html .
     */
    private String appName;

    /**
     * the url of reporting executed command.
     * If user configures the statUrl, it will be showed when use the command of session.
     */
    private String statUrl;

    /**
     * session timeout seconds.
     * After this time, the user will be logged out from Arthas.
     * @see ShellServerOptions#DEFAULT_SESSION_TIMEOUT
     */
    private Long sessionTimeout;

    /**
     * disabled commands.
     * If the user configures a disabled command, the command will not be available in Arthas.
     * If more than one is needed, separate by ','.
     */
    private String disabledCommands;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public long getJavaPid() {
        return javaPid;
    }

    public void setJavaPid(long javaPid) {
        this.javaPid = javaPid;
    }

    public String getArthasAgent() {
        return arthasAgent;
    }

    public void setArthasAgent(String arthasAgent) {
        this.arthasAgent = arthasAgent;
    }

    public String getArthasCore() {
        return arthasCore;
    }

    public void setArthasCore(String arthasCore) {
        this.arthasCore = arthasCore;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getTunnelServer() {
        return tunnelServer;
    }

    public void setTunnelServer(String tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getStatUrl() {
        return statUrl;
    }

    public void setStatUrl(String statUrl) {
        this.statUrl = statUrl;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getEnhanceLoaders() {
        return enhanceLoaders;
    }

    public void setEnhanceLoaders(String enhanceLoaders) {
        this.enhanceLoaders = enhanceLoaders;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisabledCommands() {
        return disabledCommands;
    }

    public void setDisabledCommands(String disabledCommands) {
        this.disabledCommands = disabledCommands;
    }

    /**
     * 序列化成字符串
     *
     * @return 序列化字符串
     */
    @Override
    public String toString() {

        final Map<String, String> map = new HashMap<String, String>();
        for (Field field : ArthasReflectUtils.getFields(Configure.class)) {

            // 过滤掉静态类
            if (isStatic(field.getModifiers())) {
                continue;
            }

            // 非静态的才需要纳入非序列化过程
            try {
                Object fieldValue = ArthasReflectUtils.getFieldValueByField(this, field);
                if (fieldValue != null) {
                    map.put(field.getName(), String.valueOf(fieldValue));
                }
            } catch (Throwable t) {
                //
            }

        }

        return FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toString(map);
    }

    /**
     * 反序列化字符串成对象
     *
     * @param toString 序列化字符串
     * @return 反序列化的对象
     */
    public static Configure toConfigure(String toString) throws IllegalAccessException {
        final Configure configure = new Configure();
        final Map<String, String> map = FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toMap(toString);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            final Field field = ArthasReflectUtils.getField(Configure.class, entry.getKey());
            if (null != field && !isStatic(field.getModifiers())) {
                ArthasReflectUtils.set(field, ArthasReflectUtils.valueOf(field.getType(), entry.getValue()), configure);
            }
        }
        return configure;
    }

}
