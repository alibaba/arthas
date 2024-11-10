package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.klass100.OgnlCommand;
import com.taobao.arthas.core.command.model.ExpressTypeEnum;


/**
 * ExpressFactory
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {
    private static final Logger logger = LoggerFactory.getLogger(ExpressFactory.class);

    private static final ThreadLocal<Express> expressRef = ThreadLocal.withInitial(() -> new OgnlExpress());
    private static final ThreadLocal<Express> expressRefQLExpress = ThreadLocal.withInitial(() -> new QLExpress());

    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static Express threadLocalExpress(Object object) {
        if (GlobalOptions.ExpressType.equals(ExpressTypeEnum.QLEXPRESS.getExpressType())) {
            return expressRefQLExpress.get().reset().bind(object);
        }
        return expressRef.get().reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        if (GlobalOptions.ExpressType.equals(ExpressTypeEnum.QLEXPRESS.getExpressType())) {
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
}