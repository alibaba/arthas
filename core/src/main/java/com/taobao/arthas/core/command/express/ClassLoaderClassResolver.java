package com.taobao.arthas.core.command.express;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ognl.ClassResolver;

/**
 * 类加载器类解析器
 *
 * 该类实现了OGNL的ClassResolver接口，用于在OGNL表达式执行时解析类。
 * 主要特点：
 * 1. 使用指定的类加载器来加载类，而不是使用默认的类加载机制
 * 2. 缓存已加载的类以提高性能
 * 3. 对于没有包名的简单类名，自动尝试加载java.lang包下的类
 *
 * 这个类使得Arthas可以使用特定类加载器来解析OGNL表达式中的类引用，
 * 在处理复杂类加载环境时非常有用。
 *
 * @author hengyunabc 2018-10-18
 * @see ognl.DefaultClassResolver
 */
public class ClassLoaderClassResolver implements ClassResolver {

    /**
     * 用于加载类的类加载器
     * 所有类都通过这个类加载器来加载
     */
    private ClassLoader classLoader;

    /**
     * 类缓存映射表
     * 使用ConcurrentHashMap保证线程安全，缓存已加载的类以提高性能
     * 键为类的全限定名，值为对应的Class对象
     */
    private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(101);

    /**
     * 构造函数：创建类加载器类解析器
     *
     * @param classLoader 用于解析类的类加载器，所有类都通过这个类加载器来加载
     */
    public ClassLoaderClassResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 根据类名加载类
     *
     * 该方法首先从缓存中查找类，如果缓存中没有则使用指定的类加载器加载。
     * 对于没有包名的简单类名（如"String"），会自动尝试加载java.lang包下的类。
     *
     * @param className 要加载的类名，可以是全限定名或简单类名
     * @param context OGNL上下文对象（当前实现未使用）
     * @return 加载的Class对象，如果找不到类则返回null
     * @throws ClassNotFoundException 如果类加载失败
     */
    @Override
    public Class classForName(String className, Map context) throws ClassNotFoundException {
        Class<?> result = null;

        // 尝试从缓存中获取类
        if ((result = classes.get(className)) == null) {
            try {
                // 缓存未命中，使用指定的类加载器加载类
                result = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                // 如果类名中没有包名（即没有'.'），尝试加载java.lang包下的类
                if (className.indexOf('.') == -1) {
                    // 例如："String" -> "java.lang.String"
                    result = Class.forName("java.lang." + className);
                    // 将java.lang包下的类缓存起来
                    classes.put("java.lang." + className, result);
                }
            }
            // 如果还是没有找到类，返回null
            if (result == null) {
                return null;
            }
            // 将加载的类放入缓存
            classes.put(className, result);
        }
        return result;
    }
}
