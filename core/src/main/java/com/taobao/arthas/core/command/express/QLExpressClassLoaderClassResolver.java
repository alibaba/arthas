package com.taobao.arthas.core.command.express;

import com.alibaba.qlexpress4.ClassSupplier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author TaoKan
 * @Date 2024/12/1 7:07 PM
 */
public class QLExpressClassLoaderClassResolver implements ClassSupplier {

    private ClassLoader classLoader;

    private final Map<String, Optional<Class<?>>> cache = new ConcurrentHashMap<>();

    public QLExpressClassLoaderClassResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private Optional<Class<?>> loadClsInner(String clsQualifiedName) {
        try {
            Class<?> aClass = null;
            if (classLoader != null) {
                aClass = classLoader.loadClass(clsQualifiedName);
            }else {
                aClass = Class.forName(clsQualifiedName);
            }
            return Optional.of(aClass);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
    @Override
    public Class<?> loadCls(String className) {
        Optional<Class<?>> clsOp = cache.computeIfAbsent(className, this::loadClsInner);
        return clsOp.orElse(null);
    }
}
