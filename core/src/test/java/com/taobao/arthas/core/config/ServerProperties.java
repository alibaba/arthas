package com.taobao.arthas.core.config;

import java.net.InetAddress;

@Config(prefix = "server")
public class ServerProperties {

    /**
     * Server HTTP port.
     */
    private Integer port;

    /**
     * Network address to which the server should bind to.
     */
    private InetAddress address;

    /**
     * Context path of the application.
     */
    private String contextPath;

    /**
     * Display name of the application.
     */
    private String displayName = "application";

    @NestedConfig
    private ErrorProperties error = new ErrorProperties();

    @NestedConfig
    private Ssl ssl;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ErrorProperties getError() {
        return error;
    }

    public void setError(ErrorProperties error) {
        this.error = error;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }
}
