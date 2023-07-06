package com.alibaba.arthas.tunnel.rpc.grpc;

import com.alibaba.arthas.tunnel.common.grpc.auto.Payload;
import com.alibaba.arthas.tunnel.common.grpc.auto.RequestGrpc;
import com.alibaba.arthas.tunnel.common.remote.request.Request;
import com.alibaba.arthas.tunnel.common.remote.request.RequestMeta;
import com.alibaba.arthas.tunnel.common.remote.response.ErrorResponse;
import com.alibaba.arthas.tunnel.common.remote.response.Response;
import com.alibaba.arthas.tunnel.common.util.GrpcUtils;
import com.alibaba.arthas.tunnel.rpc.remote.RequestHandler;
import com.alibaba.arthas.tunnel.rpc.remote.RequestHandlerRegistry;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author qiyue.zhang@aloudata.com
 * @description GrpcRequestAcceptor
 * @date 2023/6/15 17:33
 */
@Service
public class GrpcRequestAcceptor extends RequestGrpc.RequestImplBase {
    
    @Autowired
    private RequestHandlerRegistry requestHandlerRegistry;
    
    @Override
    public void request(Payload request, StreamObserver<Payload> responseObserver) {
        RequestHandler handler = requestHandlerRegistry.getByRequestType(request.getMetadata().getType());
        if (handler == null) {
            Payload payloadResponse = GrpcUtils
                    .convert(ErrorResponse.build(500, "RequestHandler Not Found"));
            responseObserver.onNext(payloadResponse);
            responseObserver.onCompleted();
            return;
        }
        Class requestClass = requestHandlerRegistry.getRequestClass(request.getMetadata().getType());
        RequestMeta requestMeta = new RequestMeta();
        Response response = handler.handleRequest((Request) GrpcUtils.parse(request, requestClass), requestMeta);
        responseObserver.onNext(GrpcUtils.convert(response));
        responseObserver.onCompleted();
    }
}
