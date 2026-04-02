package com.alibaba.arthas.tunnel.server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;

/**
 * 客户端连接信息类
 * <p>
 * 该类用于存储已连接到Tunnel Server的客户端的基本信息和通信上下文。
 * 客户端是指需要通过Tunnel Server访问Agent的应用程序，如浏览器、命令行工具等。
 * </p>
 * <p>
 * 该类的一个重要功能是通过Promise机制实现异步等待Agent连接。
 * 当客户端请求连接某个Agent，但该Agent尚未连接到Tunnel Server时，
 * 系统会创建一个Promise对象，客户端可以等待该Promise完成。
 * 当Agent连接上来后，Promise会被设置为成功，客户端的等待就会结束。
 * </p>
 *
 * @author hengyunabc 2019-08-27
 *
 */
public class ClientConnectionInfo {

    /**
     * Netty通道处理器上下文
     * <p>
     * 用于管理客户端与Tunnel Server之间的网络连接和通信。
     * 使用@JsonIgnore注解避免在序列化为JSON时包含该字段，因为ChannelHandlerContext不可序列化。
     * </p>
     */
    @JsonIgnore
    private ChannelHandlerContext channelHandlerContext;

    /**
     * 客户端连接到Tunnel Server时使用的IP地址
     */
    private String host;

    /**
     * 客户端连接到Tunnel Server时使用的端口号
     */
    private int port;

    /**
     * 等待Agent连接的Promise对象
     * <p>
     * 该字段用于实现异步等待Agent的连接。当客户端请求连接某个Agent时：
     * <ul>
     * <li>如果Agent已经连接，直接建立客户端与Agent之间的数据转发通道</li>
     * <li>如果Agent尚未连接，创建一个Promise对象，客户端等待该Promise完成</li>
     * </ul>
     * 当Agent连接上来后，对应的Promise会被设置为成功，Promise的结果就是Agent的Channel对象。
     * </p>
     * <p>
     * 使用@JsonIgnore注解避免在序列化为JSON时包含该字段。
     * </p>
     */
    @JsonIgnore
    private Promise<Channel> promise;

    /**
     * 获取Netty通道处理器上下文
     * <p>
     * 通过该上下文可以向客户端发送消息，或者获取连接的相关信息。
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
     * 在客户端成功连接到Tunnel Server后，会将连接的上下文设置到该对象中，
     * 以便后续通过该上下文与客户端进行通信。
     * </p>
     *
     * @param channelHandlerContext Netty通道处理器上下文对象
     */
    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    /**
     * 获取等待Agent连接的Promise对象
     * <p>
     * 通过该Promise对象，客户端可以异步等待Agent的连接。
     * 当Agent连接上来后，可以通过promise.setSuccess(agentChannel)来通知等待的客户端。
     * </p>
     *
     * @return Promise对象，结果是Agent的Channel
     */
    public Promise<Channel> getPromise() {
        return promise;
    }

    /**
     * 设置等待Agent连接的Promise对象
     * <p>
     * 当客户端请求连接一个尚未连接的Agent时，会创建一个Promise对象并设置到该字段。
     * 后续可以通过该Promise来通知客户端Agent已经连接。
     * </p>
     *
     * @param promise Promise对象，用于异步等待Agent连接
     */
    public void setPromise(Promise<Channel> promise) {
        this.promise = promise;
    }

    /**
     * 获取客户端的IP地址
     *
     * @return 客户端的IP地址字符串
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置客户端的IP地址
     *
     * @param host 客户端的IP地址字符串
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取客户端的端口号
     *
     * @return 客户端的端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置客户端的端口号
     *
     * @param port 客户端的端口号
     */
    public void setPort(int port) {
        this.port = port;
    }
}
