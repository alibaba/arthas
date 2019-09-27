package com.taobao.arthas.core.util;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author hengyunabc 2019-02-05
 *
 */
public class ClassLoaderUtils {

    public static Set<ClassLoader> getAllClassLoader(Instrumentation inst) {
        Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                classLoaderSet.add(classLoader);
            }
        }
        return classLoaderSet;
    }

    public static ClassLoader getClassLoader(Instrumentation inst, String hashCode) {
        if (hashCode == null || hashCode.isEmpty()) {
            return null;
        }

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                if (Integer.toHexString(classLoader.hashCode()).equals(hashCode)) {
                    return classLoader;
                }
            }
        }
        return null;
    }

    public static String classLoaderHash(ClassLoader classLoader) {
        int hashCode = 0;
        if (classLoader == null) {
            hashCode = System.identityHashCode(classLoader);
        } else {
            hashCode = classLoader.hashCode();
        }
        if (hashCode <= 0) {
            hashCode = System.identityHashCode(classLoader);
            if (hashCode < 0) {
                hashCode = hashCode & Integer.MAX_VALUE;
            }
        }
        return Integer.toHexString(hashCode);
    }
}
