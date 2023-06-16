package com.alibaba.arthas.tunnel.rpc.remote;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author qiyue.zhang@aloudata.com
 * @description RequestHandlerRegistry
 * @date 2023/6/15 19:05
 */
@Service
public class RequestHandlerRegistry {

    private Map<String, RequestHandler> requestHandlerMap = new HashMap<>();
    
    public RequestHandlerRegistry(List<RequestHandler> requestHandlers) {
        for (RequestHandler requestHandler : requestHandlers) {
            requestHandlerMap.put(requestHandler.getType(), requestHandler);
        }
    }
    
    public RequestHandler getByRequestType(String type) {
        return requestHandlerMap.get(type);
    }
    
    public Class getRequestClass(String type) {
        RequestHandler handler = requestHandlerMap.get(type);
        if (handler != null) {
            return handler.getRequestClass();
        } else {
            return null;
        }
    }
    
}
