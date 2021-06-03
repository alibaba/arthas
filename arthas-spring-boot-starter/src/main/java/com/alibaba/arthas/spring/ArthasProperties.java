package com.alibaba.arthas.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 
 * @author hengyunabc 2020-06-23
 *
 */
@ConfigurationProperties(prefix = "arthas")
public class ArthasProperties {
	private String ip;
	private int telnetPort;
	private int httpPort;

	private String tunnelServer;
	private String agentId;

	private String appName;

	/**
	 * report executed command
	 */
	private String statUrl;

	/**
	 * session timeout seconds
	 */
	private long sessionTimeout;

	private String home;

	/**
	 * when arthas agent init error will throw exception by default.
	 */
	private boolean slientInit = false;

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public boolean isSlientInit() {
		return slientInit;
	}

	public void setSlientInit(boolean slientInit) {
		this.slientInit = slientInit;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getTelnetPort() {
		return telnetPort;
	}

	public void setTelnetPort(int telnetPort) {
		this.telnetPort = telnetPort;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
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

	public long getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
