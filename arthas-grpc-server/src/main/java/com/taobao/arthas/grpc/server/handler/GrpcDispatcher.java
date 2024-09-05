package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/9/6 01:12
 */

import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/9/6 01:12
 * @description: GrpcDelegrate
 */
public class GrpcDispatcher {

    private static final String GRPC_SERVICE_PACKAGE_NAME = "com.taobao.arthas.grpc.server.service.impl";

    public static void loadGrpcService(){
        List<Class<?>> classes = ReflectUtil.findClasses(GRPC_SERVICE_PACKAGE_NAME);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(GrpcService.class)) {
                try {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
//                    Map<String, Method> ;
                    System.out.println("实例化类: " + clazz.getName());
                    // 你可以在这里调用实例的方法或进行其他操作
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void execute(String serviceName,String methodName, Object[] args){
//        // 创建一个 Lookup 对象
//        MethodHandles.Lookup lookup = MethodHandles.lookup();
//
//        // 获取方法句柄
//        MethodType methodType = MethodType.methodType(void.class, String.class);
//        MethodHandle methodHandle = lookup.findVirtual(Example.class, "sayHello", methodType);
//
//        // 调用方法句柄
//        methodHandle.invoke(example, "World");  // 输出: Hello, World
    }
}
