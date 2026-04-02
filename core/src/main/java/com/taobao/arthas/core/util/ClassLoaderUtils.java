package com.taobao.arthas.core.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 * 类加载器工具类
 * 提供类加载器的查询、获取和操作功能
 *
 * @author hengyunabc 2019-02-05
 *
 */
public class ClassLoaderUtils {
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ClassLoaderUtils.class);

    /**
     * 获取所有已加载的类加载器
     * 通过Java Instrumentation API遍历所有已加载的类，提取其类加载器
     *
     * @param inst Java Instrumentation实例，用于获取所有已加载的类
     * @return 所有类加载器的集合（不包含null，即不包含Bootstrap ClassLoader）
     */
    public static Set<ClassLoader> getAllClassLoader(Instrumentation inst) {
        // 使用Set避免重复的类加载器
        Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();

        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 获取加载该类的类加载器
            ClassLoader classLoader = clazz.getClassLoader();
            // 只添加非null的类加载器（Bootstrap ClassLoader为null）
            if (classLoader != null) {
                classLoaderSet.add(classLoader);
            }
        }
        return classLoaderSet;
    }

    /**
     * 根据hashCode获取类加载器
     * 通过类加载器的十六进制hashCode值查找对应的类加载器实例
     *
     * @param inst     Java Instrumentation实例
     * @param hashCode 类加载器的十六进制hashCode字符串
     * @return 匹配的类加载器，如果未找到则返回null
     */
    public static ClassLoader getClassLoader(Instrumentation inst, String hashCode) {
        // 参数校验：hashCode不能为null或空
        if (hashCode == null || hashCode.isEmpty()) {
            return null;
        }

        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                // 将类加载器的hashCode转换为十六进制字符串进行比较
                if (Integer.toHexString(classLoader.hashCode()).equals(hashCode)) {
                    return classLoader;
                }
            }
        }
        // 未找到匹配的类加载器
        return null;
    }

    /**
     * 通过类加载器的类名查找对应的类加载器实例
     * 根据类加载器的完整类名查找所有匹配的类加载器实例
     *
     * @param inst                  Java Instrumentation实例
     * @param classLoaderClassName  类加载器的完整类名（如"java.net.URLClassLoader"）
     * @return 匹配的类加载器列表，如果未找到则返回空列表
     */
    public static List<ClassLoader> getClassLoaderByClassName(Instrumentation inst, String classLoaderClassName) {
        // 参数校验：类名不能为null或空
        if (classLoaderClassName == null || classLoaderClassName.isEmpty()) {
            return null;
        }

        // 获取所有类加载器
        Set<ClassLoader> classLoaderSet = getAllClassLoader(inst);

        // 存储匹配的类加载器
        List<ClassLoader> matchClassLoaders = new ArrayList<ClassLoader>();

        // 遍历所有类加载器，查找类名匹配的
        for (ClassLoader classLoader : classLoaderSet) {
            // 比较类加载器的实际类名
            if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                matchClassLoaders.add(classLoader);
            }
        }
        return matchClassLoaders;
    }

    /**
     * 计算类加载器的哈希值（十六进制字符串）
     * 用于唯一标识一个类加载器实例
     *
     * @param classLoader 类加载器实例，可以为null（表示Bootstrap ClassLoader）
     * @return 十六进制的哈希字符串，永远为正值
     */
    public static String classLoaderHash(ClassLoader classLoader) {
        int hashCode = 0;

        // 如果类加载器为null（Bootstrap ClassLoader），使用identityHashCode
        if (classLoader == null) {
            hashCode = System.identityHashCode(classLoader);
        } else {
            // 否则使用对象本身的hashCode
            hashCode = classLoader.hashCode();
        }

        // 确保哈希值为正数
        // 如果hashCode <= 0，使用identityHashCode并转为正数
        if (hashCode <= 0) {
            hashCode = System.identityHashCode(classLoader);
            if (hashCode < 0) {
                // 将负数转为正数（使用位与运算）
                hashCode = hashCode & Integer.MAX_VALUE;
            }
        }

        // 返回十六进制字符串表示
        return Integer.toHexString(hashCode);
    }

    /**
     * 通过类加载器的类名或toString()返回值查找类加载器列表
     * 支持三种模式：
     * 1. 仅按类名查找
     * 2. 仅按toString()值查找
     * 3. 同时按类名和toString()值查找（取交集）
     *
     * @param inst                  Java Instrumentation实例
     * @param classLoaderClassName  类加载器的类名，可以为null
     * @param classLoaderToString   类加载器的toString()返回值，可以为null
     * @return 匹配的类加载器列表
     */
    public static List<ClassLoader> getClassLoader(Instrumentation inst, String classLoaderClassName, String classLoaderToString) {
        List<ClassLoader> matchClassLoaders = new ArrayList<ClassLoader>();

        // 如果两个参数都为空，直接返回空列表
        if (StringUtils.isEmpty(classLoaderClassName) && StringUtils.isEmpty(classLoaderToString)) {
            return matchClassLoaders;
        }

        // 获取所有类加载器
        Set<ClassLoader> classLoaderSet = getAllClassLoader(inst);

        // 存储按toString()匹配的类加载器（用于求交集）
        List<ClassLoader> matchedByClassLoaderToStr = new ArrayList<ClassLoader>();

        // 遍历所有类加载器进行匹配
        for (ClassLoader classLoader : classLoaderSet) {
            // 情况1：只提供了classLoaderClassName
            if (!StringUtils.isEmpty(classLoaderClassName) && StringUtils.isEmpty(classLoaderToString)) {
                if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                    matchClassLoaders.add(classLoader);
                }
            }
            // 情况2：只提供了classLoaderToString
            else if (!StringUtils.isEmpty(classLoaderToString) && StringUtils.isEmpty(classLoaderClassName)) {
                if (classLoader.toString().equals(classLoaderToString)) {
                    matchClassLoaders.add(classLoader);
                }
            }
            // 情况3：同时提供了classLoaderClassName和classLoaderToString
            else {
                // 先分别收集匹配的结果
                if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                    matchClassLoaders.add(classLoader);
                }
                if (classLoader.toString().equals(classLoaderToString)) {
                    matchedByClassLoaderToStr.add(classLoader);
                }
            }
        }

        // 如果同时提供了两个条件，取交集（同时满足两个条件的类加载器）
        if (!StringUtils.isEmpty(classLoaderClassName) && !StringUtils.isEmpty(classLoaderToString)) {
            matchClassLoaders.retainAll(matchedByClassLoaderToStr);
        }
        return matchClassLoaders;
    }

    /**
     * 获取类加载器的URL路径列表
     * 支持URLClassLoader和JDK9+的内部类加载器
     * 对于JDK9+，使用Unsafe反射访问私有字段
     *
     * @param classLoader 类加载器实例
     * @return URL数组，表示类加载器的类路径，如果获取失败则返回null
     */
    @SuppressWarnings({ "unchecked", "restriction" })
    public static URL[] getUrls(ClassLoader classLoader) {
        // 处理URLClassLoader（JDK8及之前的标准方式）
        if (classLoader instanceof URLClassLoader) {
            try {
                return ((URLClassLoader) classLoader).getURLs();
            } catch (Throwable e) {
                logger.error("classLoader: {} getUrls error", classLoader, e);
            }
        }

        // 处理JDK9+的内部类加载器
        // JDK9开始，URLClassLoader被内部实现替代，需要使用反射访问
        if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            try {
                // 使用Unsafe来访问私有字段
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

                // 获取ucp（URLClassPath）字段的所有者类
                Class<?> ucpOwner = classLoader.getClass();
                Field ucpField = null;

                // 向上遍历类继承层次，查找ucp字段
                // jdk 9~15: jdk.internal.loader.ClassLoaders$AppClassLoader.ucp
                // jdk 16~17: jdk.internal.loader.BuiltinClassLoader.ucp
                while (ucpField == null && !ucpOwner.getName().equals("java.lang.Object")) {
                    try {
                        ucpField = ucpOwner.getDeclaredField("ucp");
                    } catch (NoSuchFieldException ex) {
                        // 当前类没有ucp字段，尝试父类
                        ucpOwner = ucpOwner.getSuperclass();
                    }
                }
                if (ucpField == null) {
                    return null;
                }

                // 使用Unsafe获取ucp字段的值
                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);
                if (ucpObject == null) {
                    return null;
                }

                // 获取URLClassPath的path字段（ArrayList<URL>）
                // jdk.internal.loader.URLClassPath.path
                Field pathField = ucpField.getType().getDeclaredField("path");
                if (pathField == null) {
                    return null;
                }
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

                // 转换为数组返回
                return path.toArray(new URL[path.size()]);
            } catch (Throwable e) {
                // ignore - 忽略异常，返回null
                return null;
            }
        }
        // 不支持的类加载器类型
        return null;
    }
}
