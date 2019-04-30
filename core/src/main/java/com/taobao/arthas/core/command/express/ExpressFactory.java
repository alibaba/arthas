package com.taobao.arthas.core.command.express;

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

    private static final MvelExpress MVEL_EXPRESS = new MvelExpress();

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
        return MVEL_EXPRESS;
    }
}