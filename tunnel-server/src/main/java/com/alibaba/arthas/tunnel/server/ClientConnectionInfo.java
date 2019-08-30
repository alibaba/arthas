package com.alibaba.arthas.tunnel.server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;

/**
 * 
 * @author hengyunabc 2019-08-27
 *
 */
public class ClientConnectionInfo {

    @JsonIgnore
    private ChannelHandlerContext channelHandlerContext;
    private String host;
    private int port;

    /**
     * wait for agent connect
     */
    @JsonIgnore
    private Promise<Channel> promise;

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    public Promise<Channel> getPromise() {
        return promise;
    }

    public void setPromise(Promise<Channel> promise) {
        this.promise = promise;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
