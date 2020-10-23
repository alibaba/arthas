package com.taobao.arthas.common;

/**
 * 
 * @author hengyunabc 2020-09-02
 *
 */
public class ArthasConstants {
    /**
     * local address in VM communication
     * 
     * @see io.netty.channel.local.LocalAddress
     * @see io.netty.channel.local.LocalChannel
     */
    public static final String NETTY_LOCAL_ADDRESS = "arthas-netty-LocalAddress";

    public static int MAX_HTTP_CONTENT_LENGTH = 1024 * 1024 * 8;

    public static final String ARTHAS_OUTPUT = "arthas-output";
}
