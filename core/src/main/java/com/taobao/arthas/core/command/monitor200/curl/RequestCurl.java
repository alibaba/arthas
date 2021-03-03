package com.taobao.arthas.core.command.monitor200.curl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zhaoyuening
 */
class RequestCurl {

    private Class clazz;
    private Object requestObj;

    public RequestCurl(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * 通过反射获取到request对象
     */
    private static Object getRequestObj(Class clazz) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> holderClass = clazz.getClassLoader().loadClass("org.springframework.web.context.request.RequestContextHolder");
        Method getRequestAttributesMethod = holderClass.getMethod("getRequestAttributes");
        Object requestAttributes = getRequestAttributesMethod.invoke(null);
        Method getRequestMethod = requestAttributes.getClass().getMethod("getRequest");
        return getRequestMethod.invoke(requestAttributes);
    }

    @Override
    public String toString() {
        try {
            Object requestObj = getRequestObj(clazz);
            if (requestObj == null) {
                return "noCurl";
            }
            return new GetCurlBuilder(requestObj).build();
        } catch (Exception skip) {
            return "noCurl";
        }
    }
}
