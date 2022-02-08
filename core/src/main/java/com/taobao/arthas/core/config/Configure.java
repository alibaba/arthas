package com.taobao.arthas.core.config;

import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.util.reflect.ArthasReflectUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;

/**
 * <pre>
 * 配置类。
 * 注意本类里的所有字段不能有默认值，否则会出现配置混乱。
 * 在 com.taobao.arthas.core.Arthas#attach 里会调用 Configure#toStrig
 * <pre>
 *
 * @author vlinux
 * @author hengyunabc 2018-11-12
 */
@Config(prefix = "arthas")
public class Configure {

    private String ip;
    private Integer telnetPort;
    private Integer httpPort;
    private Long javaPid;
    private String arthasCore;
    private String arthasAgent;

    private String tunnelServer;
    private String agentId;

    private String username;
    private String password;

    /**
     * @see com.taobao.arthas.common.ArthasConstants#ARTHAS_OUTPUT
     */
    private String outputPath;

    /**
     * 需要被增强的ClassLoader的全类名，多个用英文 , 分隔
     */
    private String enhanceLoaders;

    /**
     * <pre>
     * 1. 如果显式传入 arthas.agentId ，则直接使用
     * 2. 如果用户没有指定，则自动尝试在查找应用的 appname，加为前缀，比如 system properties设置 project.name是 demo，则
     *    生成的 agentId是  demo-xxxx
     * </pre>
     */
    private String appName;
    /**
     * report executed command
     */
    private String statUrl;

    /**
     * session timeout seconds
     * @see ShellServerOptions#DEFAULT_SESSION_TIMEOUT
     */
    private Long sessionTimeout;

    /**
     * disabled commands
     */
    private String disabledCommands;

    /**
     * 本地连接不需要鉴权，即使配置了password。arthas.properties 里默认为true
     */
    private Boolean localConnectionNonAuth;

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

    public boolean isLocalConnectionNonAuth() {
        return localConnectionNonAuth != null && localConnectionNonAuth;
    }

    public void setLocalConnectionNonAuth(boolean localConnectionNonAuth) {
        this.localConnectionNonAuth = localConnectionNonAuth;
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
