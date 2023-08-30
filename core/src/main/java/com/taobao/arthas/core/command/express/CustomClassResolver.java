package com.taobao.arthas.core.command.express;

import ognl.ClassResolver;
import ognl.OgnlContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author diecui1202 on 2017/9/29.
 * @see ognl.DefaultClassResolver
 */
public class CustomClassResolver implements ClassResolver {

    public static final CustomClassResolver customClassResolver = new CustomClassResolver();

    private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(101);

    private CustomClassResolver() {

    }

    @Override
    public <T> Class<T> classForName(String className, OgnlContext ognlContext) throws ClassNotFoundException {
        Class<?> result = null;

        if ((result = classes.get(className)) == null) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader != null) {
                    result = classLoader.loadClass(className);
                } else {
                    result = Class.forName(className);
                }
            } catch (ClassNotFoundException ex) {
                if (className.indexOf('.') == -1) {
                    result = Class.forName("java.lang." + className);
                    classes.put("java.lang." + className, result);
                }
            }
            classes.put(className, result);
        }
        return (Class<T>) result;
    }
}
