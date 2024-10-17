package com.taobao.arthas.grpc.server.handler;


import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
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

    public static final String DEFAULT_GRPC_SERVICE_PACKAGE_NAME = "com.taobao.arthas.grpc.server.service.impl";

    public static Map<String, MethodHandle> grpcMethodInvokeMap = new HashMap<>();

    public static Map<String, MethodHandle> requestParseFromMap = new HashMap<>();
    public static Map<String, MethodHandle> requestToByteArrayMap = new HashMap<>();

    public static Map<String, MethodHandle> responseParseFromMap = new HashMap<>();
    public static Map<String, MethodHandle> responseToByteArrayMap = new HashMap<>();

    public static Map<String, Boolean> grpcMethodStreamMap = new HashMap<>();

    public void loadGrpcService(String grpcServicePackageName) {
        List<Class<?>> classes = ReflectUtil.findClasses(Optional.ofNullable(grpcServicePackageName).orElse(DEFAULT_GRPC_SERVICE_PACKAGE_NAME));
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
                            MethodHandle grpcInvoke = lookup.unreflect(method);
                            Class<?> requestClass = grpcInvoke.type().parameterType(1);
                            Class<?> responseClass = grpcInvoke.type().returnType();
                            MethodHandle requestParseFrom = lookup.findStatic(requestClass, "parseFrom", MethodType.methodType(requestClass, byte[].class));
                            MethodHandle responseParseFrom = lookup.findStatic(responseClass, "parseFrom", MethodType.methodType(responseClass, byte[].class));
                            MethodHandle requestToByteArray = lookup.findVirtual(requestClass, "toByteArray", MethodType.methodType(byte[].class));
                            MethodHandle responseToByteArray = lookup.findVirtual(responseClass, "toByteArray", MethodType.methodType(byte[].class));
                            String grpcMethodKey = generateGrpcMethodKey(grpcService.value(), grpcMethod.value());
                            grpcMethodInvokeMap.put(grpcMethodKey, grpcInvoke.bindTo(instance));
                            grpcMethodStreamMap.put(grpcMethodKey, grpcMethod.stream());
                            requestParseFromMap.put(grpcMethodKey, requestParseFrom);
                            responseParseFromMap.put(grpcMethodKey, responseParseFrom);
                            requestToByteArrayMap.put(grpcMethodKey, requestToByteArray);
                            responseToByteArrayMap.put(grpcMethodKey, responseToByteArray);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public GrpcResponse execute(String service, String method, byte[] arg) throws Throwable {
        MethodHandle methodHandle = grpcMethodInvokeMap.get(generateGrpcMethodKey(service, method));
        MethodType type = grpcMethodInvokeMap.get(generateGrpcMethodKey(service, method)).type();
        Object req = requestParseFromMap.get(generateGrpcMethodKey(service, method)).invoke(arg);
        Object execute = methodHandle.invoke(req);
        GrpcResponse grpcResponse = new GrpcResponse();
        grpcResponse.setClazz(type.returnType());
        grpcResponse.setService(service);
        grpcResponse.setMethod(method);
        grpcResponse.writeResponseData(execute);
        return grpcResponse;
    }

    public GrpcResponse execute(GrpcRequest request) throws Throwable {
        String service = request.getService();
        String method = request.getMethod();
        return this.execute(service, method, request.readData());
    }

    /**
     * 获取指定 service method 对应的入参类型
     *
     * @param serviceName
     * @param methodName
     * @return
     */
    public static Class<?> getRequestClass(String serviceName, String methodName) {
        //protobuf 规范只能有单入参
        return Optional.ofNullable(grpcMethodInvokeMap.get(generateGrpcMethodKey(serviceName, methodName))).orElseThrow(() -> new RuntimeException("The specified grpc method does not exist")).type().parameterArray()[0];
    }

    public static String generateGrpcMethodKey(String serviceName, String methodName) {
        return serviceName + "." + methodName;
    }

    public static void checkGrpcStream(GrpcRequest request) {
        request.setStream(
                Optional.ofNullable(grpcMethodStreamMap.get(generateGrpcMethodKey(request.getService(), request.getMethod())))
                        .orElse(false)
        );
        request.setStreamFirstData(true);
    }
}
