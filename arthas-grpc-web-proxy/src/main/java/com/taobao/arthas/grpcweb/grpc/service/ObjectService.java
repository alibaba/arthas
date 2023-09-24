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
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.arthas.grpcweb.grpc.Test;
import io.grpc.stub.StreamObserver;

import java.io.File;

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
    public void getInstance(ArthasService.ObjectRequest request, StreamObserver<ArthasService.StringValue> observerResponse){
        String express = request.getExpress();
        int expand = request.getExpand();

        VmTool vmTool= initVmTool();
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Test test = new Test(3);
        Object[] instances = vmTool.getInstances(Test.class);
        Object value = instances;
        if (express != null) {
            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
            try {
                value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
            } catch (ExpressException e) {
                logger.warn("ognl: failed execute express: " + express, e);

            }
        }
        ObjectVO objectVO = new ObjectVO(value, expand);
        String resultStr = StringUtils.objectToString(objectVO.needExpand() ? new ObjectView(objectVO).draw() : objectVO.getObject());

        ArthasService.StringValue response = ArthasService.StringValue.newBuilder().setValue(resultStr).build();
        observerResponse.onNext(response);
        observerResponse.onCompleted();
    }

}
