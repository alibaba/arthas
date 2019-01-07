package com.taobao.arthas.core.command.express;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ognl.ClassResolver;

/**
 *
 * @author hengyunabc 2018-10-18
 * @see ognl.DefaultClassResolver
 */
public class ClassLoaderClassResolver implements ClassResolver {

    private ClassLoader classLoader;

    private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(101);

    public ClassLoaderClassResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<?> classForName(String className, @SuppressWarnings("rawtypes") Map context)
                    throws ClassNotFoundException {
        Class<?> result = null;

        if ((result = classes.get(className)) == null) {
            try {
                result = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                if (className.indexOf('.') == -1) {
                    result = Class.forName("java.lang." + className);
                    classes.put("java.lang." + className, result);
                }
            }
            classes.put(className, result);
        }
        return result;
    }

}
