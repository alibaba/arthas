package com.taobao.arthas.grpcweb.grpc.service;

import arthas.VmTool;
import arthas.grpc.api.ArthasService;
import arthas.grpc.api.ObjectServiceGrpc;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.VmToolUtils;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import io.grpc.stub.StreamObserver;

import java.io.File;

import static com.taobao.arthas.grpcweb.grpc.objectUtils.JavaObjectConverter.toJavaObject;

public class ObjectService extends ObjectServiceGrpc.ObjectServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ObjectService.class);

    public static VmTool initVmTool() {
        File path = new File(VmTool.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
        String libPath = new File(path, VmToolUtils.detectLibName()).getAbsolutePath();
        return VmTool.getInstance(libPath);
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

    @Override
    public void getInstance(ArthasService.ObjectRequest request, StreamObserver<ArthasService.JavaObject> observerResponse){
        String express = request.getExpress();
        int expand = request.getExpand();

        VmTool vmTool= initVmTool();
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        String className = "com.taobao.arthas.grpcweb.grpc.objectUtils.ComplexObject";
        if (className == null || className.equals("")) {
            logger.error("The className option cannot be empty!");
            return;
        }
        Class matchClass;
        try{
            matchClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException: {}", className);
            throw new RuntimeException(e);
        }
        Object[] instances = vmTool.getInstances(matchClass);
        Object value = instances;
        if (express != null) {
            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
            try {
                value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
            } catch (ExpressException e) {
                logger.warn("ognl: failed execute express: " + express, e);
            }
        }
        int depth;
        if(expand > 3){
            depth = 0;
        }else if (expand > 0 && expand <= 3){
            depth = 3 - expand;
        }else {
            depth = 2;
        }
        ArthasService.JavaObject javaObject = toJavaObject(value, depth);
        observerResponse.onNext(javaObject);
        observerResponse.onCompleted();
    }

}
