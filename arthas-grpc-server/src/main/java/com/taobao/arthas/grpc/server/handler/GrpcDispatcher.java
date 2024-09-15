package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/9/6 01:12
 */

import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.protobuf.ProtobufCodec;
import com.taobao.arthas.grpc.server.protobuf.ProtobufProxy;
import com.taobao.arthas.grpc.server.utils.ByteUtil;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author: FengYe
 * @date: 2024/9/6 01:12
 * @description: GrpcDelegrate
 */
public class GrpcDispatcher {

    private static final String GRPC_SERVICE_PACKAGE_NAME = "com.taobao.arthas.grpc.server.service.impl";

    private Map<String, MethodHandle> grpcMethodInvokeMap = new HashMap<>();

    private Map<String, Boolean> grpcMethodStreamMap = new HashMap<>();

    public void loadGrpcService() {
        List<Class<?>> classes = ReflectUtil.findClasses(GRPC_SERVICE_PACKAGE_NAME);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(GrpcService.class)) {
                try {
                    // 处理 service
                    GrpcService grpcService = clazz.getAnnotation(GrpcService.class);
                    Object instance = clazz.getDeclaredConstructor().newInstance();

                    // 处理 method
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    Method[] declaredMethods = clazz.getDeclaredMethods();
                    for (Method method : declaredMethods) {
                        if (method.isAnnotationPresent(GrpcMethod.class)) {
                            GrpcMethod grpcMethod = method.getAnnotation(GrpcMethod.class);
                            MethodHandle methodHandle = lookup.unreflect(method);
                            String grpcMethodKey = generateGrpcMethodKey(grpcService.value(), grpcMethod.value());
                            grpcMethodInvokeMap.put(grpcMethodKey, methodHandle.bindTo(instance));
                            grpcMethodStreamMap.put(grpcMethodKey, grpcMethod.stream());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String generateGrpcMethodKey(String serviceName, String methodName) {
        return serviceName + "." + methodName;
    }

    public Object execute(String serviceName, String methodName, Object arg) throws Throwable {
        MethodHandle methodHandle = grpcMethodInvokeMap.get(generateGrpcMethodKey(serviceName, methodName));
        return methodHandle.invoke(arg);
    }

    public GrpcResponse execute(GrpcRequest request) throws Throwable {
        String service = request.getService();
        String method = request.getMethod();
        MethodType type = grpcMethodInvokeMap.get(generateGrpcMethodKey(request.getService(), request.getMethod())).type();
        // protobuf 规范只能有单入参
        request.setClazz(type.parameterArray()[0]);
        ProtobufCodec protobufCodec = ProtobufProxy.getCodecCacheSide(request.getClazz());
        Object decode = protobufCodec.decode(ByteUtil.getBytes(request.getByteData()));

        Object execute = this.execute(service, method, decode);

        GrpcResponse grpcResponse = new GrpcResponse();
        grpcResponse.setClazz(type.returnType());
        grpcResponse.writeResponseData(execute);
        return grpcResponse;
    }

    public void checkGrpcStream(GrpcRequest request){
        request.setStream(
                Optional.ofNullable(grpcMethodStreamMap.get(generateGrpcMethodKey(request.getService(), request.getMethod())))
                        .orElse(false)
        );
    }
}
