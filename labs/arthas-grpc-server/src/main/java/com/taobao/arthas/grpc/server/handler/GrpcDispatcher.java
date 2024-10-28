package com.taobao.arthas.grpc.server.handler;


import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    public static final String DEFAULT_GRPC_SERVICE_PACKAGE_NAME = "com.taobao.arthas.grpc.server.service.impl";

    public static Map<String, MethodHandle> grpcInvokeMap = new HashMap<>();

//    public static Map<String, StreamObserver> clientStreamInvokeMap = new HashMap<>();

    public static Map<String, MethodHandle> requestParseFromMap = new HashMap<>();

    public static Map<String, MethodHandle> requestToByteArrayMap = new HashMap<>();

    public static Map<String, MethodHandle> responseParseFromMap = new HashMap<>();

    public static Map<String, MethodHandle> responseToByteArrayMap = new HashMap<>();

    public static Map<String, GrpcInvokeTypeEnum> grpcInvokeTypeMap = new HashMap<>();

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
                            String grpcMethodKey = generateGrpcMethodKey(grpcService.value(), grpcMethod.value());
                            grpcInvokeTypeMap.put(grpcMethodKey, grpcMethod.grpcType());
                            grpcInvokeMap.put(grpcMethodKey, grpcInvoke.bindTo(instance));


                            Class<?> requestClass = null;
                            Class<?> responseClass = null;
                            if (GrpcInvokeTypeEnum.UNARY.equals(grpcMethod.grpcType())) {
                                requestClass = grpcInvoke.type().parameterType(1);
                                responseClass = grpcInvoke.type().returnType();
                            } else if (GrpcInvokeTypeEnum.CLIENT_STREAM.equals(grpcMethod.grpcType()) || GrpcInvokeTypeEnum.BI_STREAM.equals(grpcMethod.grpcType())) {
                                responseClass = getInnerGenericClass(method.getGenericParameterTypes()[0]);
                                requestClass = getInnerGenericClass(method.getGenericReturnType());
                            } else if (GrpcInvokeTypeEnum.SERVER_STREAM.equals(grpcMethod.grpcType())) {
                                requestClass = getInnerGenericClass(method.getGenericParameterTypes()[0]);
                                responseClass = getInnerGenericClass(method.getGenericParameterTypes()[1]);
                            }
                            MethodHandle requestParseFrom = lookup.findStatic(requestClass, "parseFrom", MethodType.methodType(requestClass, byte[].class));
                            MethodHandle responseParseFrom = lookup.findStatic(responseClass, "parseFrom", MethodType.methodType(responseClass, byte[].class));
                            MethodHandle requestToByteArray = lookup.findVirtual(requestClass, "toByteArray", MethodType.methodType(byte[].class));
                            MethodHandle responseToByteArray = lookup.findVirtual(responseClass, "toByteArray", MethodType.methodType(byte[].class));
                            requestParseFromMap.put(grpcMethodKey, requestParseFrom);
                            responseParseFromMap.put(grpcMethodKey, responseParseFrom);
                            requestToByteArrayMap.put(grpcMethodKey, requestToByteArray);
                            responseToByteArrayMap.put(grpcMethodKey, responseToByteArray);


//                            switch (grpcMethod.grpcType()) {
//                                case UNARY:
//                                    unaryInvokeMap.put(grpcMethodKey, grpcInvoke.bindTo(instance));
//                                    return;
//                                case CLIENT_STREAM:
//                                    Object invoke = grpcInvoke.bindTo(instance).invoke();
//                                    if (!(invoke instanceof StreamObserver)) {
//                                        throw new RuntimeException(grpcMethodKey + " return class is not StreamObserver!");
//                                    }
//                                    clientStreamInvokeMap.put(grpcMethodKey, (StreamObserver) invoke);
//                                    return;
//                                case SERVER_STREAM:
//                                    return;
//                                case BI_STREAM:
//                                    return;
//                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error("GrpcDispatcher loadGrpcService error.", e);
                }
            }
        }
    }

    public GrpcResponse doUnaryExecute(String service, String method, byte[] arg) throws Throwable {
        MethodHandle methodHandle = grpcInvokeMap.get(generateGrpcMethodKey(service, method));
        MethodType type = grpcInvokeMap.get(generateGrpcMethodKey(service, method)).type();
        Object req = requestParseFromMap.get(generateGrpcMethodKey(service, method)).invoke(arg);
        Object execute = methodHandle.invoke(req);
        GrpcResponse grpcResponse = new GrpcResponse();
        grpcResponse.setClazz(type.returnType());
        grpcResponse.setService(service);
        grpcResponse.setMethod(method);
        grpcResponse.writeResponseData(execute);
        return grpcResponse;
    }

    public GrpcResponse unaryExecute(GrpcRequest request) throws Throwable {
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        MethodType type = grpcInvokeMap.get(request.getGrpcMethodKey()).type();
        Object req = requestParseFromMap.get(request.getGrpcMethodKey()).invoke(request.readData());
        Object execute = methodHandle.invoke(req);
        GrpcResponse grpcResponse = new GrpcResponse();
        grpcResponse.setClazz(type.returnType());
        grpcResponse.setService(request.getService());
        grpcResponse.setMethod(request.getMethod());
        grpcResponse.writeResponseData(execute);
        return grpcResponse;
    }

    public StreamObserver<GrpcRequest> clientStreamExecute(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) throws Throwable {
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        return (StreamObserver<GrpcRequest>) methodHandle.invoke(responseObserver);
    }

    public void serverStreamExecute(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) throws Throwable {
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        Object req = requestParseFromMap.get(request.getGrpcMethodKey()).invoke(request.readData());
        methodHandle.invoke(req, responseObserver);
    }

    public StreamObserver<GrpcRequest> biStreamExecute(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) throws Throwable {
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        return (StreamObserver<GrpcRequest>) methodHandle.invoke(responseObserver);
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
        return Optional.ofNullable(grpcInvokeMap.get(generateGrpcMethodKey(serviceName, methodName))).orElseThrow(() -> new RuntimeException("The specified grpc method does not exist")).type().parameterArray()[0];
    }

    public static String generateGrpcMethodKey(String serviceName, String methodName) {
        return serviceName + "." + methodName;
    }

    public static void checkGrpcType(GrpcRequest request) {
        request.setGrpcType(
                Optional.ofNullable(grpcInvokeTypeMap.get(generateGrpcMethodKey(request.getService(), request.getMethod())))
                        .orElse(GrpcInvokeTypeEnum.UNARY)
        );
        request.setStreamFirstData(true);
    }

    public static Class<?> getInnerGenericClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] actualTypeArguments = paramType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                Type innerType = actualTypeArguments[0]; // 获取第一个实际类型参数
                if (innerType instanceof ParameterizedType) {
                    return getInnerGenericClass(innerType); // 递归调用获取最内层类型
                } else if (innerType instanceof Class) {
                    return (Class<?>) innerType; // 直接返回 Class 类型
                }
            }
        }
        return null; // 如果没有找到对应的类型
    }
}
