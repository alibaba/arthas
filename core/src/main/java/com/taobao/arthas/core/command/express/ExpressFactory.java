package com.taobao.arthas.core.command.express;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ExpressFactory
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {

    private static final ThreadLocal<Express> EXPRESS_REF = new ThreadLocal<Express>() {
        @Override
        protected Express initialValue() {
            return new OgnlExpress();
        }
    };

    private static final ConcurrentHashMap<String, MvelExpress> MVEL_EXPRESS = new ConcurrentHashMap<String, MvelExpress>();

    /**
     * get ThreadLocal Express Object
     * @param object obj
     * @return express
     */
    public static Express threadLocalExpress(Object object) {
        return EXPRESS_REF.get().reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }

    public static Express mvelExpress(ClassLoader classloader) {
        String classLoaderName = classloader.getClass().getName();
        MvelExpress express = MVEL_EXPRESS.get(classLoaderName);
        if (express == null) {
            express = new MvelExpress(classloader);
            MVEL_EXPRESS.put(classLoaderName, express);
        }
        return express;
    }
}