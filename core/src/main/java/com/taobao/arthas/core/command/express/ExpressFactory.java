package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.util.CleanableThreadLocal;

/**
 * ExpressFactory
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {

    private static final CleanableThreadLocal<Express> expressRef = new CleanableThreadLocal<Express>() {
        @Override
        protected Express initialValue() {
            return new OgnlExpress();
        }
    };

    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static Express threadLocalExpress(Object object) {
        return expressRef.get().reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }

    /**
     * Clean up all thread-bound Express instances.
     */
    public static void cleanUp() {
        expressRef.cleanUp();
    }
}
