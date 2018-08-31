package com.taobao.arthas.core.command.express;

import ognl.ClassResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * @author diecui1202 on 2017/9/29.
 */
public class CustomClassResolver implements ClassResolver {

    public static final CustomClassResolver customClassResolver = new CustomClassResolver();

    private static final ThreadLocal<ClassLoader> classLoader = new ThreadLocal<ClassLoader>();

    private Map classes = new HashMap(101);

    private CustomClassResolver() {

    }

    public Class classForName(String className, Map context) throws ClassNotFoundException {
        Class result = null;

        if ((result = (Class) classes.get(className)) == null) {
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
        return result;
    }
}
