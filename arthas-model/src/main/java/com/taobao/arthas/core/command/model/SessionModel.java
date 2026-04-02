package com.taobao.arthas.core.command.model;

/**
 * 会话命令结果模型
 * 用于表示Arthas客户端连接会话的相关信息
 * 包含Java进程ID、会话ID、代理ID、隧道服务器等连接信息
 *
 * @author gongdewei 2020/03/27
 */
public class SessionModel extends ResultModel {

    /**
     * Java进程ID
     * 标识被监控的Java进程
     */
    private long javaPid;

    /**
     * 会话ID
     * 用于唯一标识一个Arthas客户端连接会话
     */
    private String sessionId;

    /**
     * 代理ID
     * 标识Arthas代理实例的唯一ID
     */
    private String agentId;

    /**
     * 隧道服务器地址
     * Arthas隧道服务器的连接地址，用于远程通信
     */
    private String tunnelServer;

    /**
     * 统计信息URL
     * 用于获取会话统计信息的URL地址
     */
    private String statUrl;

    /**
     * 用户ID
     * 当前连接会话对应的用户标识
     */
    private String userId;

    /**
     * 隧道连接状态
     * 标识是否已连接到隧道服务器
     */
    private boolean tunnelConnected;

    /**
     * 获取结果类型
     *
     * @return 返回"session"字符串，标识这是一个会话类型的结果
     */
    @Override
    public String getType() {
        return "session";
    }

    /**
     * 获取Java进程ID
     *
     * @return Java进程ID
     */
    public long getJavaPid() {
        return javaPid;
    }

    /**
     * 设置Java进程ID
     *
     * @param javaPid 要设置的Java进程ID
     */
    public void setJavaPid(long javaPid) {
        this.javaPid = javaPid;
    }

    /**
     * 获取会话ID
     *
     * @return 会话ID字符串
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID
     *
     * @param sessionId 要设置的会话ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取代理ID
     *
     * @return 代理ID字符串
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 设置代理ID
     *
     * @param agentId 要设置的代理ID
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * 获取隧道服务器地址
     *
     * @return 隧道服务器地址字符串
     */
    public String getTunnelServer() {
        return tunnelServer;
    }

    /**
     * 设置隧道服务器地址
     *
     * @param tunnelServer 要设置的隧道服务器地址
     */
    public void setTunnelServer(String tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    /**
     * 获取统计信息URL
     *
     * @return 统计信息URL字符串
     */
    public String getStatUrl() {
        return statUrl;
    }

    /**
     * 设置统计信息URL
     *
     * @param statUrl 要设置的统计信息URL
     */
    public void setStatUrl(String statUrl) {
        this.statUrl = statUrl;
    }

    /**
     * 判断隧道连接状态
     *
     * @return 如果已连接到隧道服务器返回true，否则返回false
     */
    public boolean isTunnelConnected() {
        return tunnelConnected;
    }

    /**
     * 设置隧道连接状态
     *
     * @param tunnelConnected 要设置的连接状态
     */
    public void setTunnelConnected(boolean tunnelConnected) {
        this.tunnelConnected = tunnelConnected;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID字符串
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户ID
     *
     * @param userId 要设置的用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
