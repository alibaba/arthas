package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/9/6 01:12
 */

import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/9/6 01:12
 * @description: GrpcDelegrate
 */
public class GrpcDispatcher {

    private static final String GRPC_SERVICE_PACKAGE_NAME = "com.taobao.arthas.grpc.server.service.impl";

    private Map<String, MethodHandle> grpcMethodMap = new HashMap<>();

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
                            GrpcMethod grpcMethod = clazz.getAnnotation(GrpcMethod.class);
                            MethodHandle methodHandle = lookup.unreflect(method);
                            methodHandle.bindTo(instance);
                            grpcMethodMap.put(generateGrpcMethodKey(grpcService.value(),grpcMethod.value()), methodHandle);
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

    public Object execute(String serviceName, String methodName, Object... args) throws Throwable {
        MethodHandle methodHandle = grpcMethodMap.get(generateGrpcMethodKey(serviceName, methodName));
        return methodHandle.invoke(args);
    }

    public GrpcResponse execute(GrpcRequest request) throws Throwable {
        String service = request.getService();
        String method = request.getMethod();

    }
}
