package com.alibaba.arthas.tunnel.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;

/**
 * 
 * @author hengyunabc 2019-08-27
 *
 */
public class ClientConnectionInfo {

    private ChannelHandlerContext channelHandlerContext;

    /**
     * wait for agent connect
     */
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

}
