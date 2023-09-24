package com.taobao.arthas.grpcweb.grpc;

import arthas.VmTool;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.VmToolUtils;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

import java.io.File;


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
        Test test = new Test(2);
        String className = "com.taobao.arthas.grpcweb.grpc.Test";
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

        String express = "instances[0].mapExample";
        int expand = 6;

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
        System.out.println(resultStr);


//        for(int i =0 ; i < 5; i++){
//            Test test = new Test(i);
//        }
//        Test[] instances = vmTool.getInstances(Test.class);
//        for(Test item: instances){
//            System.out.println(item);
//        }
    }
}
