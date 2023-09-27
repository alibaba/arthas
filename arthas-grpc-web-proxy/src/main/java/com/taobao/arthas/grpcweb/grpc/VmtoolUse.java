package com.taobao.arthas.grpcweb.grpc;

import arthas.VmTool;
import arthas.grpc.api.ArthasService;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.VmToolUtils;
import com.taobao.arthas.grpcweb.grpc.objectUtils.ComplexObject;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ObjectVO;
//import com.taobao.arthas.core.util.StringUtils;
//import com.taobao.arthas.core.view.ObjectView;

import java.io.File;

import static com.taobao.arthas.grpcweb.grpc.Test.ccc;
import static com.taobao.arthas.grpcweb.grpc.objectUtils.JavaObjectConverter.toJavaObject;


/**
 * @program: arthas
 * *
 * @author: XY
 * @create: 2023-09-22 14:15
 **/

public class VmtoolUse {

    private static final Logger logger = LoggerFactory.getLogger(VmtoolUse.class);

    private static final String ARTHAS_AGENT = "com.taobao.arthas.agent.attach.ArthasAgent";

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

    public static void main(String[] args) throws ClassNotFoundException {
        VmTool vmTool= initVmTool();
//        Test test = new Test(2);
        ComplexObject ccc = ccc();
        String className = "com.taobao.arthas.grpcweb.grpc.objectUtils.ComplexObject";
        if (className == null || className.equals("")) {
            logger.error("The className option cannot be empty!");
            return;
        }
        // classLoader
        ClassLoader classLoader = null;
        classLoader = ClassLoader.getSystemClassLoader();
//        Class arthasAgentClass = Class.forName(ARTHAS_AGENT);

        Class matchClass = Class.forName(className);
        Object[] instances = vmTool.getInstances(matchClass);
        Object value = instances;

        String express = "instances[0]";
        int expand = 3;
        int depth;

        if (express != null) {
            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
            try {
                value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
            } catch (ExpressException e) {
                logger.warn("ognl: failed execute express: " + express, e);
            }
        }
        if(expand > 3){
            depth = 0;
        }else if (expand > 0 && expand <= 3){
            depth = 3 - expand;
        }else {
            depth = 2;
        }
        ArthasService.JavaObject javaObject = toJavaObject(value, depth);
        System.err.println(javaObject);
        vmTool.forceGc();
    }
}
