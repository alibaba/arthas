package com.alibaba.arthas.tunnel.server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.netty.channel.ChannelHandlerContext;

/**
 * Agent信息类
 * <p>
 * 该类用于存储已连接到Tunnel Server的Agent的基本信息和通信上下文。
 * 每个Agent连接到Tunnel Server后，都会创建一个对应的AgentInfo对象来维护其状态。
 * </p>
 *
 * @author hengyunabc 2019-08-27
 *
 */
public class AgentInfo {

    /**
     * Netty通道处理器上下文
     * <p>
     * 用于管理Agent与Tunnel Server之间的网络连接和通信。
     * 使用@JsonIgnore注解避免在序列化为JSON时包含该字段，因为ChannelHandlerContext不可序列化。
     * </p>
     */
    @JsonIgnore
    private ChannelHandlerContext channelHandlerContext;

    /**
     * Agent连接到Tunnel Server时使用的IP地址
     * <p>
     * 该地址用于在网络中唯一标识Agent的位置。
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
     * 获取Netty通道处理器上下文
     * <p>
     * 通过该上下文可以发送消息给Agent，或者获取连接的相关信息。
     * </p>
     *
     * @return Netty通道处理器上下文对象
     */
    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    /**
     * 设置Netty通道处理器上下文
     * <p>
     * 在Agent成功连接到Tunnel Server后，会将连接的上下文设置到该对象中，
     * 以便后续通过该上下文与Agent进行通信。
     * </p>
     *
     * @param channelHandlerContext Netty通道处理器上下文对象
     */
    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    /**
     * 获取Agent的IP地址
     *
     * @return Agent的IP地址字符串
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置Agent的IP地址
     *
     * @param host Agent的IP地址字符串
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取Agent的端口号
     *
     * @return Agent的端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置Agent的端口号
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

}
