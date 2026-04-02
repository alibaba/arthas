package com.alibaba.arthas.tunnel.server;

/**
 * Agent集群信息类
 * <p>
 * 该类用于存储Agent在集群环境中的连接信息，包括Agent自身的连接信息以及客户端连接到的Tunnel Server信息。
 * 主要用于支持分布式部署场景，当有多个Tunnel Server时，客户端需要知道连接到哪个Tunnel Server才能访问特定的Agent。
 * </p>
 *
 * @author hengyunabc 2020-10-30
 *
 */
public class AgentClusterInfo {

    /**
     * Agent连接到Tunnel Server时使用的IP地址
     * <p>
     * 这是Agent主动连接到Tunnel Server时使用的网络地址，用于标识Agent在网络中的位置。
     * </p>
     */
    private String host;

    /**
     * Agent连接到Tunnel Server时使用的端口号
     */
    private int port;

    /**
     * Arthas版本号
     * <p>
     * 记录当前Agent使用的Arthas版本信息，用于版本兼容性检查和问题排查。
     * </p>
     */
    private String arthasVersion;

    /**
     * 客户端应该连接的Tunnel Server的IP地址
     * <p>
     * 在集群环境中，可能有多个Tunnel Server实例。客户端需要通过这个地址连接到特定的Tunnel Server，
     * 该Tunnel Server与目标Agent建立了连接。这样可以实现负载均衡和高可用性。
     * </p>
     */
    private String clientConnectHost;

    /**
     * 客户端应该连接的Tunnel Server的端口号
     */
    private int clientConnectTunnelPort;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的AgentClusterInfo对象，所有字段都需要通过setter方法后续设置。
     * </p>
     */
    public AgentClusterInfo() {

    }

    /**
     * 通过AgentInfo对象构造AgentClusterInfo对象
     * <p>
     * 该构造函数从已有的AgentInfo对象中提取Agent的连接信息，并结合客户端连接信息创建完整的集群信息对象。
     * </p>
     *
     * @param agentInfo Agent信息对象，包含Agent的主机、端口和版本信息
     * @param clientConnectHost 客户端应该连接的Tunnel Server的IP地址
     * @param clientConnectTunnelPort 客户端应该连接的Tunnel Server的端口号
     */
    public AgentClusterInfo(AgentInfo agentInfo, String clientConnectHost, int clientConnectTunnelPort) {
        // 从AgentInfo中复制Agent的连接信息
        this.host = agentInfo.getHost();
        this.port = agentInfo.getPort();
        this.arthasVersion = agentInfo.getArthasVersion();
        // 设置客户端连接信息
        this.clientConnectHost = clientConnectHost;
        this.clientConnectTunnelPort = clientConnectTunnelPort;
    }

    /**
     * 获取Agent连接到Tunnel Server时使用的IP地址
     *
     * @return Agent的IP地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置Agent连接到Tunnel Server时使用的IP地址
     *
     * @param host Agent的IP地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取Agent连接到Tunnel Server时使用的端口号
     *
     * @return Agent的端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置Agent连接到Tunnel Server时使用的端口号
     *
     * @param port Agent的端口号
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取Arthas版本号
     *
     * @return Arthas版本字符串
     */
    public String getArthasVersion() {
        return arthasVersion;
    }

    /**
     * 设置Arthas版本号
     *
     * @param arthasVersion Arthas版本字符串
     */
    public void setArthasVersion(String arthasVersion) {
        this.arthasVersion = arthasVersion;
    }

    /**
     * 获取客户端应该连接的Tunnel Server的IP地址
     *
     * @return Tunnel Server的IP地址
     */
    public String getClientConnectHost() {
        return clientConnectHost;
    }

    /**
     * 设置客户端应该连接的Tunnel Server的IP地址
     *
     * @param clientConnectHost Tunnel Server的IP地址
     */
    public void setClientConnectHost(String clientConnectHost) {
        this.clientConnectHost = clientConnectHost;
    }

    /**
     * 获取客户端应该连接的Tunnel Server的端口号
     *
     * @return Tunnel Server的端口号
     */
    public int getClientConnectTunnelPort() {
        return clientConnectTunnelPort;
    }

    /**
     * 设置客户端应该连接的Tunnel Server的端口号
     *
     * @param clientConnectTunnelPort Tunnel Server的端口号
     */
    public void setClientConnectTunnelPort(int clientConnectTunnelPort) {
        this.clientConnectTunnelPort = clientConnectTunnelPort;
    }

}
