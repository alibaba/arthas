package com.alibaba.arthas.tunnel.common;

/**
 * 隧道通信方法常量定义
 *
 * <p>该类定义了tunnel client和tunnel server之间通信时使用的各种方法常量。</p>
 * <p>客户端和服务器通过URI中的method参数来区分不同的通信行为和操作类型。</p>
 *
 * <p>通信流程：</p>
 * <ol>
 *   <li>Agent启动时使用agentRegister方法注册到Tunnel Server</li>
 *   <li>浏览器通过connectArthas方法请求连接到Agent</li>
 *   <li>Tunnel Server通过startTunnel方法通知Agent创建新连接</li>
 *   <li>Agent使用openTunnel方法建立到Tunnel Server的连接</li>
 *   <li>Tunnel Server通过httpProxy方法请求Agent转发HTTP请求</li>
 * </ol>
 *
 * @author hengyunabc 2020-10-22
 *
 */
public class MethodConstants {

    /**
     * Agent注册方法
     *
     * <p>该方法用于tunnel client启动时向tunnel server注册自己。</p>
     *
     * <p>使用方式：</p>
     * <pre>
     * tunnel client启动时注册：
     * ws://192.168.1.10:7777/ws?method=agentRegister
     *
     * tunnel server回应示例：
     * response:/?method=agentRegister&amp;id=bvDOe8XbTM2pQWjF4cfw
     *
     * 注意：
     * - id参数由tunnel server生成，用于唯一标识该agent
     * - 如果请求中未指定id，则server会随机生成一个
     * - 该id后续用于连接到特定的agent实例
     * </pre>
     */
    public static final String AGENT_REGISTER = "agentRegister";

    /**
     * 启动隧道方法
     *
     * <p>该方法用于tunnel server通知tunnel client启动一个新的连接通道。</p>
     * <p>通常在浏览器请求连接到某个agent时触发。</p>
     *
     * <p>使用方式：</p>
     * <pre>
     * tunnel server 通知 tunnel client启动新连接：
     * response:/?method=startTunnel&amp;id=bvDOe8XbTM2pQWjF4cfw&amp;clientConnectionId=AMku9EFz2gxeL2gedGOC
     *
     * 参数说明：
     * - id: agent的唯一标识符（注册时生成）
     * - clientConnectionId: 为这次连接生成的唯一连接ID，用于标识具体的客户端连接
     * </pre>
     */
    public static final String START_TUNNEL = "startTunnel";

    /**
     * 连接Arthas方法
     *
     * <p>该方法用于browser通知tunnel server去连接指定的tunnel client（agent）。</p>
     * <p>这是用户通过浏览器访问Arthas控制台的入口点。</p>
     *
     * <p>使用方式：</p>
     * <pre>
     * browser 通知tunnel server连接到agent：
     * ws://192.168.1.10:7777/ws?method=connectArthas&amp;id=bvDOe8XbTM2pQWjF4cfw
     *
     * 参数说明：
     * - id: 要连接的agent的唯一标识符
     * </pre>
     */
    public static final String CONNECT_ARTHAS = "connectArthas";

    /**
     * 打开隧道方法
     *
     * <p>该方法用于tunnel client收到startTunnel指令后，创建到tunnel server的新连接。</p>
     * <p>这个连接用于实际的数据传输。</p>
     *
     * <p>使用方式：</p>
     * <pre>
     * tunnel client收到 startTunnel 指令之后，以下面的 URI新建一个连接：
     * ws://127.0.0.1:7777/ws/?method=openTunnel&amp;clientConnectionId=AMku9EFz2gxeL2gedGOC&amp;id=bvDOe8XbTM2pQWjF4cfw
     *
     * 参数说明：
     * - id: agent的唯一标识符
     * - clientConnectionId: 客户端连接ID（从startTunnel指令中获取）
     * </pre>
     */
    public static final String OPEN_TUNNEL = "openTunnel";

    /**
     * HTTP代理方法
     *
     * <p>该方法用于tunnel server向tunnel client请求HTTP请求中转。</p>
     * <p>主要用于访问本地Arthas服务器提供的HTTP资源。</p>
     *
     * <p>使用场景：</p>
     * <pre>
     * tunnel server向 tunnel client请求 http中转，例如访问：
     * http://localhost:3658/arthas-output/xxx.html
     *
     * 应用场景：
     * - 浏览器通过tunnel server访问Arthas的输出结果
     * - 查看异步任务的结果页面
     * - 访问本地Arthas服务器的其他HTTP端点
     * </pre>
     */
    public static final String HTTP_PROXY = "httpProxy";

}
