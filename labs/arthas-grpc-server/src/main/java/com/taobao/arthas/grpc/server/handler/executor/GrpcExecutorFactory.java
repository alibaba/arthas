package com.taobao.arthas.grpc.server.handler.executor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/10/24 01:56
 * @description: GrpcExecutorFactory
 */
public class GrpcExecutorFactory {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    public static final String DEFAULT_GRPC_EXECUTOR_PACKAGE_NAME = "com.taobao.arthas.grpc.server.handler.executor";

    private final Map<GrpcInvokeTypeEnum, GrpcExecutor> map = new HashMap<>();

    public void loadExecutor(GrpcDispatcher dispatcher) {
        List<Class<?>> classes = ReflectUtil.findClasses(DEFAULT_GRPC_EXECUTOR_PACKAGE_NAME);
        for (Class<?> clazz : classes) {
            if (GrpcExecutor.class.isAssignableFrom(clazz)) {
                try {
                    if (AbstractGrpcExecutor.class.equals(clazz) || GrpcExecutor.class.equals(clazz)) {
                        continue;
                    }
                    if (AbstractGrpcExecutor.class.isAssignableFrom(clazz)) {
                        Constructor<?> constructor = clazz.getConstructor(GrpcDispatcher.class);
                        GrpcExecutor executor = (GrpcExecutor) constructor.newInstance(dispatcher);
                        map.put(executor.supportGrpcType(), executor);
                    } else {
                        Constructor<?> constructor = clazz.getConstructor();
                        GrpcExecutor executor = (GrpcExecutor) constructor.newInstance();
                        map.put(executor.supportGrpcType(), executor);
                    }
                } catch (Exception e) {
                    logger.error("GrpcExecutorFactory loadExecutor error", e);
                }
            }
        }
    }

    public GrpcExecutor getExecutor(GrpcInvokeTypeEnum grpcType) {
        return map.get(grpcType);
    }
}
