package com.taobao.arthas.grpcweb.grpc.service;

import arthas.VmTool;
import arthas.grpc.api.ArthasService;
import arthas.grpc.api.ObjectServiceGrpc;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.VmToolUtils;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.arthas.grpcweb.grpc.model.WatchRequestModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.grpcweb.grpc.service.advisor.WatchRpcAdviceListener;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.taobao.arthas.grpcweb.grpc.objectUtils.JavaObjectConverter.toJavaObject;

public class ObjectService extends ObjectServiceGrpc.ObjectServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ObjectService.class);

    private Instrumentation instrumentation;

    private GrpcJobController grpcJobController;


    public ObjectService(GrpcJobController grpcJobController) {
        this.instrumentation = grpcJobController.getInstrumentation();
        this.grpcJobController = grpcJobController;
    }

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
    public void getInstance(ArthasService.ObjectRequest request, StreamObserver<ArthasService.ResponseBody> responseObserver){
        ArthasStreamObserver<ArthasService.ResponseBody> newArthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, null,grpcJobController);

        int jobId = request.getJobId();
        long resultId = request.getResultId();
        String type = request.getType();
        String express = request.getExpress();
        int expand = request.getExpand();

        if(grpcJobController.containsJob(jobId)){
            ArthasStreamObserver oldArthasStreamObserver = grpcJobController.getGrpcJob(jobId);
            if(oldArthasStreamObserver != null){
                WatchRpcAdviceListener listener = (WatchRpcAdviceListener) AdviceWeaver.listener(oldArthasStreamObserver.getListener().id());
                try {
                    ObjectVO newObjectValue = (ObjectVO) listener.getNewObjectValue(resultId, express, expand);
                    String result = StringUtils.objectToString(
                            newObjectValue.needExpand() ? new ObjectView(((WatchRequestModel) oldArthasStreamObserver.getRequestModel()).getSizeLimit(), newObjectValue).draw() : newObjectValue.getObject());
                    ArthasService.ResponseBody responseBody  = ArthasService.ResponseBody.newBuilder()
                            .setJobId(jobId)
                            .setResultId(resultId)
                            .setType(type)
                            .setStringValue(result)
                            .build();
                    newArthasStreamObserver.onNext(responseBody);
                    newArthasStreamObserver.onCompleted();
                } catch (ExpressException e) {
                    logger.error("Express Exception ERROR");
                    throw new RuntimeException(e);
                }
            }

//            int depth;
//            if(expand > 3){
//                depth = 0;
//            }else if (expand > 0 && expand <= 3){
//                depth = 3 - expand;
//            }else {
//                depth = 2;
//            }
//            ArthasService.JavaObject javaObject = toJavaObject(value, depth);
//            arthasStreamObserver.onNext(javaObject);
//            arthasStreamObserver.onCompleted();
        }

//        Instrumentation inst = instrumentation;


//        VmTool vmTool= initVmTool();
//
//        if (className == null || className.equals("")) {
//            logger.error("The className option cannot be empty!");
//            arthasStreamObserver.onCompleted();
//            return;
//        }
//
//        ClassLoader classLoader = null;
//        if (hashCode != null && !"".equals(hashCode)) {
//            classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
//            if (classLoader == null) {
//                logger.error("Can not find classloader with hashCode: " + hashCode + ".");
//                arthasStreamObserver.onCompleted();
//                return;
//            }
//        }else if ( classLoaderClass != null && !"".equals(classLoaderClass)) {
//            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst,
//                    classLoaderClass);
//            if (matchedClassLoaders.size() == 1) {
//                classLoader = matchedClassLoaders.get(0);
//                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
//            } else if (matchedClassLoaders.size() > 1) {
//                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
//                        .createClassLoaderVOList(matchedClassLoaders);
//                logger.info("Found more than one classloader by class name: classLoaderClass: {}, MatchedClassLoaders: {}",
//                        classLoaderClass, classLoaderVOList);
//                arthasStreamObserver.onCompleted();
//                return;
//            } else {
//                logger.error("Can not find classloader by class name: " + classLoaderClass + ".");
//                arthasStreamObserver.onCompleted();
//                return;
//            }
//        }else {
//            classLoader = ClassLoader.getSystemClassLoader();
//        }
//
//        List<Class<?>> matchedClasses = new ArrayList<Class<?>>(
//                SearchUtils.searchClassOnly(inst, className, false, hashCode));
//        int matchedClassSize = matchedClasses.size();
//        if (matchedClassSize == 0) {
//            logger.warn("Can not find class by class name: " + className + ".");
//            arthasStreamObserver.onCompleted();
//            return;
//        } else if (matchedClassSize > 1) {
//            logger.warn("Found more than one class: " + matchedClasses + ", please specify classloader with '-c <classloader hash>'");
//            return;
//        } else {
//            Object[] instances = vmTool.getInstances(matchedClasses.get(0), limit);
//            Object value = instances;
//            if (express != null) {
//                Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
//                try {
//                    value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
//                } catch (ExpressException e) {
//                    logger.warn("ognl: failed execute express: " + express, e);
//                }
//            }
//            int depth;
//            if(expand > 3){
//                depth = 0;
//            }else if (expand > 0 && expand <= 3){
//                depth = 3 - expand;
//            }else {
//                depth = 2;
//            }
//            ArthasService.JavaObject javaObject = toJavaObject(value, depth);
//            arthasStreamObserver.onNext(javaObject);
//            arthasStreamObserver.onCompleted();
//        }
    }

}
