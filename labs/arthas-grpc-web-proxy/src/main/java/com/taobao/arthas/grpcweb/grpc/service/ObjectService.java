package com.taobao.arthas.grpcweb.grpc.service;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.grpcweb.grpc.objectUtils.JavaObjectConverter;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.VmToolUtils;

import arthas.VmTool;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.observer.impl.ArthasStreamObserverImpl;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.arthas.api.ObjectServiceGrpc.ObjectServiceImplBase;
import io.arthas.api.ArthasServices.JavaObject;
import io.arthas.api.ArthasServices.ObjectQuery;
import io.arthas.api.ArthasServices.ObjectQueryResult;
import io.arthas.api.ArthasServices.ObjectQueryResult.Builder;

public class ObjectService extends ObjectServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private VmTool vmTool;
    private Instrumentation inst;

    private GrpcJobController grpcJobController;


    public ObjectService(GrpcJobController grpcJobController, String libDir) {
        this.inst = grpcJobController.getInstrumentation();
        this.grpcJobController = grpcJobController;

        try {
            String detectLibName = VmToolUtils.detectLibName();
            String vmToolLibPath = Paths.get(libDir, detectLibName).toString();

            vmTool = VmTool.getInstance(vmToolLibPath);
        } catch (Throwable e) {
            logger.error("init vmtool error", e);
        }
    }

    @Override
    public void query(ObjectQuery query, StreamObserver<ObjectQueryResult> responseObserver) {
        if (vmTool == null) {
            throw Status.UNAVAILABLE.withDescription("vmtool can not work").asRuntimeException();
        }
        ArthasStreamObserver<ObjectQueryResult> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, null,grpcJobController);
        String className = query.getClassName();
        String classLoaderHash = query.getClassLoaderHash();
        String classLoaderClass = query.getClassLoaderClass();
        int limit = query.getLimit();
        int depth = query.getDepth();
        String express = query.getExpress();
        String resultExpress = query.getResultExpress();

        // 如果只传递了 class name 参数，则jvm 里可能有多个同名的 class，需要全部查找
        if (isEmpty(classLoaderHash) && isEmpty(classLoaderClass)) {
            List<Class<?>> foundClassList = new ArrayList<>();
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (clazz.getName().equals(className)) {
                    foundClassList.add(clazz);
                }
            }

            // 没找到
            if (foundClassList.size() == 0) {
                arthasStreamObserver.onNext(ObjectQueryResult.newBuilder().setSuccess(false)
                        .setMessage("can not find class: " + className).build());
                arthasStreamObserver.onCompleted();
                return;
            } else if (foundClassList.size() > 1) {
                String message = "found more than one class: " + className;
                arthasStreamObserver.onNext(ObjectQueryResult.newBuilder().setSuccess(false).setMessage(message).build());
                arthasStreamObserver.onCompleted();
                return;
            } else { // 找到了指定的 类
                Object[] instances = vmTool.getInstances(foundClassList.get(0), limit);
                Builder builder = ObjectQueryResult.newBuilder().setSuccess(true);
                /**
                 *  这里尝试使用express
                 */
                Object value = null;
                if (!isEmpty(express)) {
                    Express unpooledExpress = ExpressFactory.unpooledExpress(foundClassList.get(0).getClassLoader());
                    try {
                        value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
                    } catch (ExpressException e) {
                        logger.warn("ognl: failed execute express: " + express, e);
                    }
                }
                if(value != null && !isEmpty(resultExpress)){
                    try {
                        value = ExpressFactory.threadLocalExpress(value).bind(Constants.COST_VARIABLE, 0.0).get(resultExpress);
                    } catch (ExpressException e) {
                        logger.warn("ognl: failed execute result express: " + express, e);
                    }
                }
                JavaObject javaObject = JavaObjectConverter.toJavaObjectWithExpand(value, depth);
                builder.addObjects(javaObject);
                arthasStreamObserver.onNext(builder.build());
                arthasStreamObserver.onCompleted();
                return;
            }
        }

        // 有指定 classloader hash 或者 classloader className

        Class<?> foundClass = null;

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (!clazz.getName().equals(className)) {
                continue;
            }

            ClassLoader classLoader = clazz.getClassLoader();

            if (classLoader == null) {
                continue;
            }

            if (!isEmpty(classLoaderHash)) {
                String hex = Integer.toHexString(classLoader.hashCode());
                if (classLoaderHash.equals(hex)) {
                    foundClass = clazz;
                    break;
                }
            }

            if (!isEmpty(classLoaderClass) && classLoaderClass.equals(classLoader.getClass().getName())) {
                foundClass = clazz;
                break;
            }
        }
        // 没找到类
        if (foundClass == null) {
            arthasStreamObserver.onNext(ObjectQueryResult.newBuilder().setSuccess(false)
                    .setMessage("can not find class: " + className).build());
            arthasStreamObserver.onCompleted();
            return;
        }

        Object[] instances = vmTool.getInstances(foundClass, limit);
        Builder builder = ObjectQueryResult.newBuilder().setSuccess(true);
//        for (Object obj : instances) {
//            JavaObject javaObject = JavaObjectConverter.toJavaObjectWithExpand(obj, depth);
//            builder.addObjects(javaObject);
//        }

        Object value = null;
        if (!isEmpty(express)) {
            Express unpooledExpress = ExpressFactory.unpooledExpress(foundClass.getClassLoader());
            try {
                value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
            } catch (ExpressException e) {
                logger.warn("ognl: failed execute express: " + express, e);
            }
        }
        if(value != null && !isEmpty(resultExpress)){
            try {
                value = ExpressFactory.threadLocalExpress(value).bind(Constants.COST_VARIABLE, 0.0).get(resultExpress);
            } catch (ExpressException e) {
                logger.warn("ognl: failed execute result express: " + express, e);
            }
        }
        JavaObject javaObject = JavaObjectConverter.toJavaObjectWithExpand(value, depth);
        builder.addObjects(javaObject);
        arthasStreamObserver.onNext(builder.build());
        arthasStreamObserver.onCompleted();
    }

    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    static class InstancesWrapper {
        Object instances;

        public InstancesWrapper(Object instances) {
            this.instances = instances;
        }

        public Object getInstances() {
            return instances;
        }

        public void setInstances(Object instances) {
            this.instances = instances;
        }
    }

}
