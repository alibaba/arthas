package com.alibaba.arthas.tunnel.common;

/**
 * URI常量定义
 *
 * <p>定义了隧道客户端和服务器之间通信时使用的各种URI参数常量</p>
 * <p>这些常量用于构建WebSocket通信的查询参数</p>
 *
 * @author hengyunabc 2020-10-22
 *
 */
public class URIConstans {

    /**
     * 方法参数名
     * <p>用于标识请求的方法类型</p>
     * <p>参考 {@link MethodConstants} 中定义的方法常量</p>
     *
     * @see MethodConstants
     */
    public static final String METHOD = "method";

    /**
     * 响应参数名
     * <p>用于标识响应数据</p>
     */
    public static final String RESPONSE = "response";

    /**
     * 代理ID
     * <p>由隧道服务器分配的唯一标识符</p>
     * <p>用于标识不同的Arthas代理实例</p>
     */
    public static final String ID = "id";

    /**
     * 客户端连接ID
     * <p>隧道服务器用于区分不同隧道客户端的内部ID</p>
     * <p>每个客户端连接都会分配一个唯一的连接ID</p>
     */
    public static final String CLIENT_CONNECTION_ID = "clientConnectionId";

    /**
     * 目标URL
     * <p>当隧道服务器向隧道客户端请求HTTP代理时，目标URL的参数名</p>
     * <p>指定需要代理的实际请求地址</p>
     *
     * @see com.alibaba.arthas.tunnel.common.MethodConstants#HTTP_PROXY
     */
    public static final String TARGET_URL = "targetUrl";

    /**
     * 代理请求ID
     * <p>用于标识一次代理请求的唯一ID</p>
     * <p>随机生成，用于匹配请求和响应</p>
     */
    public static final String PROXY_REQUEST_ID = "requestId";

    /**
     * 代理响应数据
     * <p>代理请求的返回值，使用Base64编码</p>
     * <p>包含HTTP代理请求的响应内容</p>
     */
    public static final String PROXY_RESPONSE_DATA = "responseData";

    /**
     * Arthas版本号
     * <p>标识Arthas的版本信息</p>
     */
    public static final String ARTHAS_VERSION = "arthasVersion";

    /**
     * 应用名称
     * <p>用于标识不同的应用实例</p>
     */
    public static final String APP_NAME = "appName";
}
