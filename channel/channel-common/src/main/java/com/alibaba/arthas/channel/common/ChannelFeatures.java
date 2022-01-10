package com.alibaba.arthas.channel.common;

public interface ChannelFeatures {

    /**
     * Support execute command
     * @see com.alibaba.arthas.channel.proto.RequestAction
     * @see com.alibaba.arthas.channel.proto.ActionRequest
     */
    String EXECUTE_COMMAND = "ExecuteCommand";

    /**
     * Support proxying web console
     * @see com.alibaba.arthas.channel.proto.RequestAction
     * @see com.alibaba.arthas.channel.proto.ActionRequest
     */
    String WEB_CONSOLE = "WebConsole";

}