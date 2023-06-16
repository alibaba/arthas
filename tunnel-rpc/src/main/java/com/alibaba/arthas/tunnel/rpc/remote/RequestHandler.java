package com.alibaba.arthas.tunnel.rpc.remote;

import com.alibaba.arthas.tunnel.common.remote.request.Request;
import com.alibaba.arthas.tunnel.common.remote.request.RequestMeta;
import com.alibaba.arthas.tunnel.common.remote.response.Response;

import java.lang.reflect.ParameterizedType;

/**
 * @author qiyue.zhang@aloudata.com
 * @description RequestHandler
 * @date 2023/6/15 17:55
 */
public abstract class RequestHandler<T extends Request, S extends Response> {
    
    public Response handleRequest(T request, RequestMeta meta) {
        return handle(request, meta);
    }
    
    public abstract S handle(T request, RequestMeta meta);
    
    public String getType() {
        return ((Class)((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getSimpleName();
    }
    
    public Class getRequestClass() {
        return (Class)((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
    
    public Class getResponseClass() {
        return (Class)((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }
    
}
