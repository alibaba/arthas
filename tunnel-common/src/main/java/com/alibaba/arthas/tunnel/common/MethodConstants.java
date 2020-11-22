package com.alibaba.arthas.tunnel.common;

/**
 * tunnel client和server之间通过 URI来通迅，在URI里定义了一个 method的参数，定义不同的行为
 * 
 * @author hengyunabc 2020-10-22
 *
 */
public class MethodConstants {

    /**
     * 
     * <pre>
     * tunnel client启动时注册的 method
     * 
     * ws://192.168.1.10:7777/ws?method=agentRegister
     * 
     * tunnel server回应：
     * 
     * response:/?method=agentRegister&id=bvDOe8XbTM2pQWjF4cfw
     * 
     * id不指定，则随机生成
     * </pre>
     */
    public static final String AGENT_REGISTER = "agentRegister";

    /**
     * <pre>
     * tunnel server 通知 tunnel client启动一个新的连接
     * 
     * response:/?method=startTunnel&id=bvDOe8XbTM2pQWjF4cfw&clientConnectionId=AMku9EFz2gxeL2gedGOC
     * </pre>
     */
    public static final String START_TUNNEL = "startTunnel";
    /**
     * <pre>
     * browser 通知tunnel server去连接 tunnel client
     * 
     * ws://192.168.1.10:7777/ws?method=connectArthas&id=bvDOe8XbTM2pQWjF4cfw
     * </pre>
     */
    public static final String CONNECT_ARTHAS = "connectArthas";

    /**
     * <pre>
     * tunnel client收到 startTunnel 指令之后，以下面的 URI新建一个连接：
     * 
     * ws://127.0.0.1:7777/ws/?method=openTunnel&clientConnectionId=AMku9EFz2gxeL2gedGOC&id=bvDOe8XbTM2pQWjF4cfw
     * </pre>
     */
    public static final String OPEN_TUNNEL = "openTunnel";
    
    /**
     * <pre>
     * tunnel server向 tunnel client请求 http中转，比如访问 http://localhost:3658/arthas-output/xxx.svg
     * </pre>
     */
    public static final String HTTP_PROXY = "httpProxy";

}
