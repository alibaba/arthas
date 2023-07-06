package com.alibaba.arthas.tunnel.rpc.remote.handler;

import com.alibaba.arthas.tunnel.common.remote.request.RequestMeta;
import com.alibaba.arthas.tunnel.common.remote.request.SimpleRequest;
import com.alibaba.arthas.tunnel.common.remote.response.SimpleResponse;
import com.alibaba.arthas.tunnel.rpc.remote.RequestHandler;
import org.springframework.stereotype.Component;

/**
 * @author qiyue.zhang@aloudata.com
 * @description SimpleRequestHandler
 * @date 2023/6/16 16:15
 */
@Component
public class SimpleRequestHandler extends RequestHandler<SimpleRequest, SimpleResponse> {
    
    @Override
    public SimpleResponse handle(SimpleRequest request, RequestMeta meta) {
        System.out.println(request.getBody());
        SimpleResponse simpleResponse = new SimpleResponse();
        simpleResponse.setBody(request.getBody());
        return simpleResponse;
    }
}
