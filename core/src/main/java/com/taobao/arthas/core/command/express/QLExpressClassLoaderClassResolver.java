package com.taobao.arthas.core.command.express;

import com.alibaba.qlexpress4.ClassSupplier;
import ognl.OgnlContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author TaoKan
 * @Date 2024/12/1 7:07 PM
 */
public class QLExpressClassLoaderClassResolver implements ClassSupplier {

    private ClassLoader classLoader;

    private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(101);

    public QLExpressClassLoaderClassResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<?> loadCls(String className) {
        Class<?> result = null;

        if ((result = classes.get(className)) == null) {
            try {
                result = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                if (className.indexOf('.') == -1) {
                    try {
                        result = Class.forName("java.lang." + className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    classes.put("java.lang." + className, result);
                }
            }
            if (result == null) {
                return null;
            }
            classes.put(className, result);
        }
        return result;
    }
}
