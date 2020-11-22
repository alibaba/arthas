package com.alibaba.arthas.tunnel.common;

/**
 * 
 * @author hengyunabc 2020-10-22
 *
 */
public class URIConstans {

    /**
     * @see MethodConstants
     */
    public static final String METHOD = "method";
    public static final String RESPONSE = "response";

    /**
     * agent id
     */
    public static final String ID = "id";

    /**
     * tunnel server用于区分不同 tunnel client的内部 id
     */
    public static final String CLIENT_CONNECTION_ID = "clientConnectionId";

    /**
     * tunnel server向 tunnel client请求http代理时的目标 url
     * 
     * @see com.alibaba.arthas.tunnel.common.MethodConstants#HTTP_PROXY
     */
    public static final String TARGET_URL = "targetUrl";

    /**
     * 标识一次proxy请求，随机生成
     */
    public static final String PROXY_REQUEST_ID = "requestId";

    /**
     * proxy请求的返回值，base64编码
     */
    public static final String PROXY_RESPONSE_DATA = "responseData";
 
    public static final String ARTHAS_VERSION = "arthasVersion";

    public static final String APP_NAME = "appName";
}
