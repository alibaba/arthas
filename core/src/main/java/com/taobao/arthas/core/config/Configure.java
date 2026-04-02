package com.taobao.arthas.core.config;

import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.util.reflect.ArthasReflectUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;

/**
 * Arthas配置类
 * <pre>
 * 该类用于存储和管理Arthas的所有配置参数。
 * 注意事项：
 * 1. 本类里的所有字段不能有默认值，否则会出现配置混乱。
 * 2. 在 com.taobao.arthas.core.Arthas#attach 里会调用 Configure#toString 进行序列化。
 * 3. 使用@Config注解指定配置前缀为"arthas"，所有配置项都以arthas开头。
 * </pre>
 *
 * @author vlinux
 * @author hengyunabc 2018-11-12
 */
@Config(prefix = "arthas")
public class Configure {

    /**
     * 监听IP地址
     * <p>
     * 指定Arthas服务器监听的IP地址，默认为0.0.0.0表示监听所有网卡。
     * 可以配置为特定IP来限制访问来源。
     */
    private String ip;

    /**
     * Telnet端口
     * <p>
     * Arthas提供的Telnet服务端口号，用于通过Telnet客户端连接Arthas。
     * 默认值为3658。
     */
    private Integer telnetPort;

    /**
     * HTTP端口
     * <p>
     * Arthas提供的HTTP API服务端口号，用于通过HTTP接口访问Arthas。
     * 默认值为8563。
     */
    private Integer httpPort;

    /**
     * Java进程ID
     * <p>
     * 要attach的目标Java进程的进程ID。
     * 通过jps命令或ps命令可以查看到Java进程的PID。
     */
    private Long javaPid;

    /**
     * Arthas核心库路径
     * <p>
     * Arthas核心库的jar包路径或URL。
     */
    private String arthasCore;

    /**
     * Arthas Agent路径
     * <p>
     * Arthas Agent的jar包路径或URL。
     */
    private String arthasAgent;

    /**
     * 隧道服务器地址
     * <p>
     * Arthas隧道服务器的地址，用于将本地Arthas连接到远程服务器。
     * 格式通常是：ws://server-host:port/websocket
     */
    private String tunnelServer;

    /**
     * Agent唯一标识
     * <p>
     * Arthas Agent的唯一标识符，用于区分不同的Agent实例。
     * 如果不指定，会根据appName自动生成，格式为：appName-随机字符串
     */
    private String agentId;

    /**
     * 用户名
     * <p>
     * 连接Arthas时需要的用户名，用于身份验证。
     */
    private String username;

    /**
     * 密码
     * <p>
     * 连接Arthas时需要的密码，用于身份验证。
     */
    private String password;

    /**
     * 输出路径
     * <p>
     * Arthas命令执行结果的输出目录路径。
     * 默认路径为：${user.home}/logs/arthas-cache
     *
     * @see com.taobao.arthas.common.ArthasConstants#ARTHAS_OUTPUT
     */
    private String outputPath;

    /**
     * 需要增强的ClassLoader
     * <p>
     * 指定需要被增强的ClassLoader的全类名，多个ClassLoader用英文逗号分隔。
     * 例如：com.alibaba.loader.ArthasClassloader
     */
    private String enhanceLoaders;

    /**
     * 应用名称
     * <pre>
     * 1. 如果显式传入 arthas.agentId，则直接使用，此字段不生效。
     * 2. 如果用户没有指定agentId，则自动尝试查找应用的appName作为前缀。
     *    例如：System Properties中设置的project.name是demo，则生成的agentId是demo-xxxx。
     *    常见的appName来源：spring.application.name、project.name等。
     * </pre>
     */
    private String appName;

    /**
     * 统计上报URL
     * <p>
     * 用于上报执行的命令统计信息的URL地址。
     * 可以用于监控和分析Arthas的使用情况。
     */
    private String statUrl;

    /**
     * 会话超时时间（秒）
     * <p>
     * Arthas客户端会话的超时时间，单位为秒。
     * 超过此时间没有操作的会话会被自动关闭。
     *
     * @see ShellServerOptions#DEFAULT_SESSION_TIMEOUT
     */
    private Long sessionTimeout;

    /**
     * 禁用的命令列表
     * <p>
     * 指定要禁用的Arthas命令，多个命令用英文逗号分隔。
     * 例如：jad,watch,tt - 这些命令将被禁用，无法执行。
     */
    private String disabledCommands;

    /**
     * 本地连接是否跳过鉴权
     * <p>
     * 如果设置为true，本地连接时即使配置了密码也不需要鉴权。
     * 在arthas.properties里默认值为true。
     * 这是一个安全特性，允许localhost连接无需密码验证。
     */
    private Boolean localConnectionNonAuth;

    /**
     * MCP端点路径
     * <p>
     * MCP (Model Context Protocol) 的端点路径。
     * 用于与支持MCP协议的客户端进行通信。
     */
    private String mcpEndpoint;

    /**
     * MCP服务器协议类型
     * <p>
     * MCP Server使用的协议类型，可选值：
     * - STREAMABLE: 流式协议
     * - STATELESS: 无状态协议
     */
    private String mcpProtocol;

    /**
     * 获取监听IP地址
     *
     * @return 监听的IP地址
     */
    public String getIp() {
        return ip;
    }

    /**
     * 设置监听IP地址
     *
     * @param ip 要监听的IP地址
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * 获取Telnet端口
     *
     * @return Telnet服务端口号
     */
    public Integer getTelnetPort() {
        return telnetPort;
    }

    /**
     * 设置Telnet端口
     *
     * @param telnetPort Telnet服务端口号
     */
    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    /**
     * 设置HTTP端口
     *
     * @param httpPort HTTP服务端口号
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * 获取HTTP端口
     *
     * @return HTTP服务端口号
     */
    public Integer getHttpPort() {
        return httpPort;
    }

    /**
     * 获取Java进程ID
     *
     * @return 目标Java进程的PID
     */
    public long getJavaPid() {
        return javaPid;
    }

    /**
     * 设置Java进程ID
     *
     * @param javaPid 目标Java进程的PID
     */
    public void setJavaPid(long javaPid) {
        this.javaPid = javaPid;
    }

    /**
     * 获取Arthas Agent路径
     *
     * @return Arthas Agent的路径
     */
    public String getArthasAgent() {
        return arthasAgent;
    }

    /**
     * 设置Arthas Agent路径
     *
     * @param arthasAgent Arthas Agent的路径
     */
    public void setArthasAgent(String arthasAgent) {
        this.arthasAgent = arthasAgent;
    }

    /**
     * 获取Arthas核心库路径
     *
     * @return Arthas核心库的路径
     */
    public String getArthasCore() {
        return arthasCore;
    }

    /**
     * 设置Arthas核心库路径
     *
     * @param arthasCore Arthas核心库的路径
     */
    public void setArthasCore(String arthasCore) {
        this.arthasCore = arthasCore;
    }

    /**
     * 获取会话超时时间
     *
     * @return 会话超时时间（秒）
     */
    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * 设置会话超时时间
     *
     * @param sessionTimeout 会话超时时间（秒）
     */
    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * 获取隧道服务器地址
     *
     * @return 隧道服务器的地址
     */
    public String getTunnelServer() {
        return tunnelServer;
    }

    /**
     * 设置隧道服务器地址
     *
     * @param tunnelServer 隧道服务器的地址
     */
    public void setTunnelServer(String tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    /**
     * 获取Agent唯一标识
     *
     * @return Agent的ID
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 设置Agent唯一标识
     *
     * @param agentId Agent的ID
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * 获取统计上报URL
     *
     * @return 统计上报的URL地址
     */
    public String getStatUrl() {
        return statUrl;
    }

    /**
     * 设置统计上报URL
     *
     * @param statUrl 统计上报的URL地址
     */
    public void setStatUrl(String statUrl) {
        this.statUrl = statUrl;
    }

    /**
     * 获取应用名称
     *
     * @return 应用的名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 设置应用名称
     *
     * @param appName 应用的名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * 获取需要增强的ClassLoader列表
     *
     * @return ClassLoader的全类名列表，用逗号分隔
     */
    public String getEnhanceLoaders() {
        return enhanceLoaders;
    }

    /**
     * 设置需要增强的ClassLoader列表
     *
     * @param enhanceLoaders ClassLoader的全类名列表，用逗号分隔
     */
    public void setEnhanceLoaders(String enhanceLoaders) {
        this.enhanceLoaders = enhanceLoaders;
    }

    /**
     * 获取输出路径
     *
     * @return 输出目录的路径
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * 设置输出路径
     *
     * @param outputPath 输出目录的路径
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取禁用的命令列表
     *
     * @return 禁用的命令，用逗号分隔
     */
    public String getDisabledCommands() {
        return disabledCommands;
    }

    /**
     * 设置禁用的命令列表
     *
     * @param disabledCommands 禁用的命令，用逗号分隔
     */
    public void setDisabledCommands(String disabledCommands) {
        this.disabledCommands = disabledCommands;
    }

    /**
     * 判断是否本地连接跳过鉴权
     * <p>
     * 只有当localConnectionNonAuth不为null且为true时才返回true。
     *
     * @return 如果本地连接跳过鉴权返回true，否则返回false
     */
    public boolean isLocalConnectionNonAuth() {
        return localConnectionNonAuth != null && localConnectionNonAuth;
    }

    /**
     * 设置本地连接是否跳过鉴权
     *
     * @param localConnectionNonAuth 是否跳过鉴权
     */
    public void setLocalConnectionNonAuth(boolean localConnectionNonAuth) {
        this.localConnectionNonAuth = localConnectionNonAuth;
    }

    /**
     * 获取MCP端点路径
     *
     * @return MCP端点路径
     */
    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    /**
     * 设置MCP端点路径
     *
     * @param mcpEndpoint MCP端点路径
     */
    public void setMcpEndpoint(String mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
    }

    /**
     * 获取MCP服务器协议类型
     *
     * @return MCP协议类型（STREAMABLE或STATELESS）
     */
    public String getMcpProtocol() {
        return mcpProtocol;
    }

    /**
     * 设置MCP服务器协议类型
     *
     * @param mcpProtocol MCP协议类型（STREAMABLE或STATELESS）
     */
    public void setMcpProtocol(String mcpProtocol) {
        this.mcpProtocol = mcpProtocol;
    }

    /**
     * 将配置对象序列化为字符串
     * <p>
     * 使用反射遍历Configure类的所有非静态字段，将字段名和值转换为Map，
     * 然后使用FeatureCodec将Map编码为命令行格式的字符串。
     * 序列化时只包含非null的字段值。
     *
     * @return 序列化后的字符串，格式为命令行参数形式
     */
    @Override
    public String toString() {

        // 创建Map存储字段名和值的映射关系
        final Map<String, String> map = new HashMap<String, String>();

        // 使用反射获取Configure类的所有字段（包括私有字段）
        for (Field field : ArthasReflectUtils.getFields(Configure.class)) {

            // 过滤掉静态字段，只处理实例字段
            if (isStatic(field.getModifiers())) {
                continue;
            }

            // 非静态字段才需要纳入序列化过程
            try {
                // 通过反射获取字段的值
                Object fieldValue = ArthasReflectUtils.getFieldValueByField(this, field);

                // 只序列化非null的字段值
                if (fieldValue != null) {
                    map.put(field.getName(), String.valueOf(fieldValue));
                }
            } catch (Throwable t) {
                // 反射获取失败时忽略该字段，继续处理其他字段
            }

        }

        // 使用FeatureCodec将Map编码为命令行格式的字符串
        return FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toString(map);
    }

    /**
     * 将字符串反序列化为配置对象
     * <p>
     * 先将命令行格式的字符串解析为Map，然后遍历Map中的每个键值对，
     * 通过反射找到对应的字段，并将值设置到Configure对象中。
     * 只处理非静态字段，且字段类型会自动转换。
     *
     * @param toString 序列化后的字符串，格式为命令行参数形式
     * @return 反序列化后的Configure对象
     * @throws IllegalAccessException 如果访问字段时没有权限
     */
    public static Configure toConfigure(String toString) throws IllegalAccessException {
        // 创建一个新的Configure对象
        final Configure configure = new Configure();

        // 使用FeatureCodec将命令行格式的字符串解析为Map
        final Map<String, String> map = FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toMap(toString);

        // 遍历Map中的每个配置项
        for (Map.Entry<String, String> entry : map.entrySet()) {
            // 根据配置项名称（key）查找对应的字段
            final Field field = ArthasReflectUtils.getField(Configure.class, entry.getKey());

            // 如果字段存在且不是静态字段，则设置字段值
            if (null != field && !isStatic(field.getModifiers())) {
                // 将字符串值转换为字段对应的类型，并设置到Configure对象中
                ArthasReflectUtils.set(field, ArthasReflectUtils.valueOf(field.getType(), entry.getValue()), configure);
            }
        }

        return configure;
    }

}
