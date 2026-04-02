package com.alibaba.arthas.spring;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Arthas配置属性类
 * <p>
 * 该类用于绑定配置文件中以"arthas"为前缀的配置属性。
 * 提供了类型安全的属性访问方式，支持在Spring配置文件中配置Arthas的各项参数。
 * </p>
 * <p>
 * 支持的配置前缀为"arthas"，例如在application.yml中：
 * <pre>
 * arthas:
 *   ip: 0.0.0.0
 *   telnet-port: 3658
 *   http-port: 8563
 * </pre>
 * </p>
 *
 * @author hengyunabc 2020-06-23
 *
 */
// 将配置文件中前缀为"arthas"的属性自动绑定到该类的字段上
@ConfigurationProperties(prefix = "arthas")
public class ArthasProperties {
	// Arthas服务绑定的IP地址，默认为0.0.0.0（监听所有网卡）
	private String ip;

	// Arthas Telnet服务端口，用于通过telnet客户端连接
	private int telnetPort;

	// Arthas HTTP服务端口，用于通过HTTP API访问Arthas功能
	private int httpPort;

	// Arthas隧道服务器地址，用于远程连接和诊断
	private String tunnelServer;

	// Arthas代理的唯一标识符，用于区分不同的应用实例
	private String agentId;

	// 应用名称，用于在Arthas控制台中标识当前应用
	private String appName;

	/**
	 * 统计上报URL
	 * <p>
	 * 用于上报Arthas执行的命令信息，便于审计和统计分析。
	 * 配置后，Arthas会将执行的命令发送到该URL。
	 * </p>
	 */
	private String statUrl;

	/**
	 * 会话超时时间（单位：秒）
	 * <p>
	 * 当客户端连接超过该时间未活动时，会话将自动断开。
	 * 用于防止僵尸会话占用资源。
	 * </p>
	 */
	private long sessionTimeout;

    // 用户名，用于Arthas的HTTP API认证
    private String username;

    // 密码，用于Arthas的HTTP API认证
    private String password;

	// Arthas安装目录的路径
	// 如果未配置，Arthas将使用默认的安装路径
	private String home;

	/**
	 * 是否静默初始化
	 * <p>
	 * 当设置为true时，Arthas agent初始化错误不会抛出异常。
	 * 默认为false，表示初始化出错时会抛出异常，导致应用启动失败。
	 * </p>
	 * <p>
	 * 这个选项适合在开发环境使用，避免因为Arthas初始化问题影响应用启动。
	 * 在生产环境建议保持为false，以便及时发现Arthas的问题。
	 * </p>
	 */
	private boolean slientInit = false;

	/**
	 * 禁用的命令列表
	 * <p>
	 * 用于指定禁用的Arthas命令，多个命令用逗号分隔。
	 * 默认禁用stop命令，防止误操作导致应用停止。
	 * </p>
	 * <p>
	 * 例如：disabledCommands: stop,jvm 可以禁用stop和jvm命令。
	 * </p>
	 */
	private String disabledCommands;

	// 默认禁用的命令，默认禁用stop命令
	// stop命令可以停止JVM，在生产环境中应该禁用
	private static final String DEFAULT_DISABLEDCOMMANDS = "stop";

    /**
     * 更新Arthas配置Map的默认值
     * <p>
     * 因为 arthasConfigMap 只注入了用户配置的值，没有默认值，因此需要统一处理补全默认值。
     * </p>
     * <p>
     * 该方法会检查配置Map中是否缺少某些必要配置，如果缺少则设置默认值。
     * 这样可以确保即使用户没有配置某些参数，Arthas也能正常运行。
     * </p>
     *
     * @param arthasConfigMap Arthas配置映射表，会被直接修改
     */
    public static void updateArthasConfigMapDefaultValue(Map<String, String> arthasConfigMap) {
		// 如果配置Map中没有disabledCommands配置项
        if (!arthasConfigMap.containsKey("disabledCommands")) {
			// 设置默认值为"stop"，禁用stop命令
            arthasConfigMap.put("disabledCommands", DEFAULT_DISABLEDCOMMANDS);
        }
    }

	/**
	 * 获取Arthas安装目录路径
	 *
	 * @return Arthas安装目录的绝对路径
	 */
	public String getHome() {
		return home;
	}

	/**
	 * 设置Arthas安装目录路径
	 *
	 * @param home Arthas安装目录的绝对路径
	 */
	public void setHome(String home) {
		this.home = home;
	}

	/**
	 * 获取是否静默初始化标志
	 *
	 * @return true表示静默初始化，false表示初始化失败会抛出异常
	 */
	public boolean isSlientInit() {
		return slientInit;
	}

	/**
	 * 设置是否静默初始化
	 *
	 * @param slientInit true表示静默初始化，false表示初始化失败会抛出异常
	 */
	public void setSlientInit(boolean slientInit) {
		this.slientInit = slientInit;
	}

	/**
	 * 获取绑定的IP地址
	 *
	 * @return IP地址
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * 设置绑定的IP地址
	 *
	 * @param ip IP地址，例如"0.0.0.0"或"127.0.0.1"
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * 获取Telnet服务端口
	 *
	 * @return Telnet端口号
	 */
	public int getTelnetPort() {
		return telnetPort;
	}

	/**
	 * 设置Telnet服务端口
	 *
	 * @param telnetPort Telnet端口号
	 */
	public void setTelnetPort(int telnetPort) {
		this.telnetPort = telnetPort;
	}

	/**
	 * 获取HTTP服务端口
	 *
	 * @return HTTP端口号
	 */
	public int getHttpPort() {
		return httpPort;
	}

	/**
	 * 设置HTTP服务端口
	 *
	 * @param httpPort HTTP端口号
	 */
	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	/**
	 * 获取隧道服务器地址
	 *
	 * @return 隧道服务器地址，格式通常为"host:port"
	 */
	public String getTunnelServer() {
		return tunnelServer;
	}

	/**
	 * 设置隧道服务器地址
	 *
	 * @param tunnelServer 隧道服务器地址，格式通常为"host:port"
	 */
	public void setTunnelServer(String tunnelServer) {
		this.tunnelServer = tunnelServer;
	}

	/**
	 * 获取代理ID
	 *
	 * @return 代理的唯一标识符
	 */
	public String getAgentId() {
		return agentId;
	}

	/**
	 * 设置代理ID
	 *
	 * @param agentId 代理的唯一标识符
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
	 * 获取会话超时时间
	 *
	 * @return 会话超时时间（秒）
	 */
	public long getSessionTimeout() {
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
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 设置应用名称
     *
     * @param appName 应用名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

	/**
	 * 获取禁用的命令列表
	 *
	 * @return 禁用的命令列表，多个命令用逗号分隔
	 */
	public String getDisabledCommands() {
		return disabledCommands;
	}

	/**
	 * 设置禁用的命令列表
	 *
	 * @param disabledCommands 禁用的命令列表，多个命令用逗号分隔
	 */
	public void setDisabledCommands(String disabledCommands) {
		this.disabledCommands = disabledCommands;
	}

    /**
     * 获取用户名
     *
     * @return HTTP API认证的用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     *
     * @param username HTTP API认证的用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码
     *
     * @return HTTP API认证的密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     *
     * @param password HTTP API认证的密码
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
