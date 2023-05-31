package com.taobao.arthas.core.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    /**
     * 通过类名查找classloader
     * @param inst
     * @param classLoaderClassName
     * @return
     */
    public static List<ClassLoader> getClassLoaderByClassName(Instrumentation inst, String classLoaderClassName) {
        if (classLoaderClassName == null || classLoaderClassName.isEmpty()) {
            return null;
        }
        Set<ClassLoader> classLoaderSet = getAllClassLoader(inst);
        List<ClassLoader> matchClassLoaders = new ArrayList<ClassLoader>();
        for (ClassLoader classLoader : classLoaderSet) {
            if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                matchClassLoaders.add(classLoader);
            }
        }
        return matchClassLoaders;
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

    /**
     * Find List<ClassLoader> by the class name of ClassLoader or the return value of ClassLoader#toString().
     * @param inst
     * @param classLoaderClassName
     * @param classLoaderToString
     * @return
     */
    public static List<ClassLoader> getClassLoader(Instrumentation inst, String classLoaderClassName, String classLoaderToString) {
        List<ClassLoader> matchClassLoaders = new ArrayList<ClassLoader>();
        if (StringUtils.isEmpty(classLoaderClassName) && StringUtils.isEmpty(classLoaderToString)) {
            return matchClassLoaders;
        }
        Set<ClassLoader> classLoaderSet = getAllClassLoader(inst);
        List<ClassLoader> matchedByClassLoaderToStr = new ArrayList<ClassLoader>();
        for (ClassLoader classLoader : classLoaderSet) {
            // only classLoaderClassName
            if (!StringUtils.isEmpty(classLoaderClassName) && StringUtils.isEmpty(classLoaderToString)) {
                if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                    matchClassLoaders.add(classLoader);
                }
            }
            // only classLoaderToString
            else if (!StringUtils.isEmpty(classLoaderToString) && StringUtils.isEmpty(classLoaderClassName)) {
                if (classLoader.toString().equals(classLoaderToString)) {
                    matchClassLoaders.add(classLoader);
                }
            }
            // classLoaderClassName and classLoaderToString
            else {
                if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                    matchClassLoaders.add(classLoader);
                }
                if (classLoader.toString().equals(classLoaderToString)) {
                    matchedByClassLoaderToStr.add(classLoader);
                }
            }
        }
        // classLoaderClassName and classLoaderToString
        if (!StringUtils.isEmpty(classLoaderClassName) && !StringUtils.isEmpty(classLoaderToString)) {
            matchClassLoaders.retainAll(matchedByClassLoaderToStr);
        }
        return matchClassLoaders;
    }

    @SuppressWarnings({ "unchecked", "restriction" })
    public static URL[] getUrls(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        // jdk9
        if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            try {
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

                Class<?> ucpOwner = classLoader.getClass();
                Field ucpField = null;

                // jdk 9~15: jdk.internal.loader.ClassLoaders$AppClassLoader.ucp
                // jdk 16~17: jdk.internal.loader.BuiltinClassLoader.ucp
                while (ucpField == null && !ucpOwner.getName().equals("java.lang.Object")) {
                    try {
                        ucpField = ucpOwner.getDeclaredField("ucp");
                    } catch (NoSuchFieldException ex) {
                        ucpOwner = ucpOwner.getSuperclass();
                    }
                }
                if (ucpField == null) {
                    return null;
                }

                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);
                if (ucpObject == null) {
                    return null;
                }

                // jdk.internal.loader.URLClassPath.path
                Field pathField = ucpField.getType().getDeclaredField("path");
                if (pathField == null) {
                    return null;
                }
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

                return path.toArray(new URL[path.size()]);
            } catch (Throwable e) {
                // ignore
                return null;
            }
        }
        return null;
    }
}
