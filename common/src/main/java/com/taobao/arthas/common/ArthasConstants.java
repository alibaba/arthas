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

    public static final int MAX_HTTP_CONTENT_LENGTH = 1024 * 1024 * 8;

    public static final String ARTHAS_OUTPUT = "arthas-output";

    public static final String APP_NAME = "app-name";

    public static final String PROJECT_NAME = "project.name";
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    public static final int TELNET_PORT = 3658;
}
