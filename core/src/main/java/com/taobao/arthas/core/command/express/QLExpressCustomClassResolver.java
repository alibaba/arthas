package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.qlexpress4.ClassSupplier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author TaoKan
 * @Date 2024/12/1 7:06 PM
 */
public class QLExpressCustomClassResolver implements ClassSupplier {

    public static final QLExpressCustomClassResolver customClassResolver = new QLExpressCustomClassResolver();

    private final Map<String, Optional<Class<?>>> cache = new ConcurrentHashMap<>();

    private QLExpressCustomClassResolver() {

    }

    private Optional<Class<?>> loadClsInner(String clsQualifiedName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> aClass = null;
            if (classLoader != null) {
                aClass = classLoader.loadClass(clsQualifiedName);
            } else {
                aClass = Class.forName(clsQualifiedName);
            }
            return Optional.of(aClass);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
    @Override
    public Class<?> loadCls(String clsQualifiedName) {
        Optional<Class<?>> clsOp = cache.computeIfAbsent(clsQualifiedName, this::loadClsInner);
        return clsOp.orElse(null);
    }

}
