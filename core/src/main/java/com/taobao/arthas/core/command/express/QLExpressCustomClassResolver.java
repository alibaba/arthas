package com.taobao.arthas.core.command.express;

import com.alibaba.qlexpress4.ClassSupplier;
import ognl.OgnlContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author TaoKan
 * @Date 2024/12/1 7:06 PM
 */
public class QLExpressCustomClassResolver implements ClassSupplier {

    public static final QLExpressCustomClassResolver customClassResolver = new QLExpressCustomClassResolver();

    private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(101);

    private QLExpressCustomClassResolver() {

    }

    @Override
    public Class<?> loadCls(String className) {
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
                    try {
                        result = Class.forName("java.lang." + className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    classes.put("java.lang." + className, result);
                }
            }
            classes.put(className, result);
        }
        return result;
    }
}
