package com.taobao.arthas.core.command.express;

import com.alibaba.fastjson2.JSON;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ExpressTypeEnum;
import com.taobao.arthas.core.command.model.QLExpressConfigModel;

/**
 * ExpressFactory
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {
    private static final ThreadLocal<Express> expressRef = ThreadLocal.withInitial(() -> new OgnlExpress());
    private static final ThreadLocal<Express> expressRefQLExpress = ThreadLocal.withInitial(() -> new QLExpress());

    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static Express threadLocalExpress(Object object) {
        if (GlobalOptions.ExpressType == ExpressTypeEnum.QLEXPRESS.getExpressType()) {
            return expressRefQLExpress.get().reset().bind(object);
        }
        return expressRef.get().reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        if (GlobalOptions.ExpressType == ExpressTypeEnum.QLEXPRESS.getExpressType()) {
            return new QLExpress(classloader);
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }

    public static Express unpooledExpressByOGNL(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }

    public static boolean checkQLExpressConfig(String configValue) {
        try {
            if ("".equals(configValue)) {
                return true;
            }
            JSON.parseObject(configValue, QLExpressConfigModel.class);
            return true;
        }catch (Throwable t){
            return false;
        }
    }
}