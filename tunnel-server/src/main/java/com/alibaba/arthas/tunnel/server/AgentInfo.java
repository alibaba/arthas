package com.alibaba.arthas.tunnel.server;

import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author hengyunabc 2019-08-27
 *
 */
public class AgentInfo {

    private ChannelHandlerContext channelHandlerContext;

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

}
