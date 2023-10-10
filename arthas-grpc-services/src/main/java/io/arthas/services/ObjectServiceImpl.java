package io.arthas.services;


import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.arthas.common.VmToolUtils;

import arthas.VmTool;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.arthas.api.ObjectServiceGrpc.ObjectServiceImplBase;
import io.arthas.api.ArthasServices.JavaObject;
import io.arthas.api.ArthasServices.ObjectQuery;
import io.arthas.api.ArthasServices.ObjectQueryResult;
import io.arthas.api.ArthasServices.ObjectQueryResult.Builder;

public class ObjectServiceImpl extends ObjectServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private VmTool vmTool;
    private Instrumentation inst;

    public ObjectServiceImpl(Instrumentation inst, String libDir) {
        this.inst = inst;

        try {
            String detectLibName = VmToolUtils.detectLibName();
            String vmToolLibPath = Paths.get(libDir, detectLibName).toString();

            logger.info("vmtool lib path: {}", vmToolLibPath);

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

        String className = query.getClassName();
        String classLoaderHash = query.getClassLoaderHash();
        String classLoaderClass = query.getClassLoaderClass();
        int limit = query.getLimit();
        int depth = query.getDepth();

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
                responseObserver.onNext(ObjectQueryResult.newBuilder().setSuccess(false)
                        .setMessage("can not find class: " + className).build());
                responseObserver.onCompleted();
                return;
            } else if (foundClassList.size() > 1) {
                String message = "found more than one class: " + className;
                responseObserver.onNext(ObjectQueryResult.newBuilder().setSuccess(false).setMessage(message).build());
                responseObserver.onCompleted();
                return;
            } else { // 找到了指定的 类
                Object[] instances = vmTool.getInstances(foundClassList.get(0), limit);
                Builder builder = ObjectQueryResult.newBuilder().setSuccess(true);
                for (Object obj : instances) {
                    JavaObject javaObject = JavaObjectConverter.toJavaObject(obj, depth);
                    builder.addObjects(javaObject);
                }
                responseObserver.onNext(builder.build());
                responseObserver.onCompleted();
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
            responseObserver.onNext(ObjectQueryResult.newBuilder().setSuccess(false)
                    .setMessage("can not find class: " + className).build());
            responseObserver.onCompleted();
            return;
        }

        Object[] instances = vmTool.getInstances(foundClass, limit);
        Builder builder = ObjectQueryResult.newBuilder().setSuccess(true);
        for (Object obj : instances) {
            JavaObject javaObject = JavaObjectConverter.toJavaObject(obj, depth);
            builder.addObjects(javaObject);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }
}
