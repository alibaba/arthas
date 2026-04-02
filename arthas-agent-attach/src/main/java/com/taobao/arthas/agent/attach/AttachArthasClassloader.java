package com.taobao.arthas.agent.attach;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Arthas Attach类加载器
 * <p>
 * 该类加载器用于在Arthas attach到目标JVM时加载Arthas的核心类。
 * 它继承自URLClassLoader，并实现了自定义的类加载策略，确保能够正确加载Arthas所需的类。
 * </p>
 *
 * @author hengyunabc 2020-06-22
 *
 */
public class AttachArthasClassloader extends URLClassLoader {

    /**
     * 构造函数
     *
     * @param urls 类加载器用于查找类文件的URL数组
     */
    public AttachArthasClassloader(URL[] urls) {
        // 使用系统类加载器的父类加载器作为父加载器
        // 这样可以确保系统类能够被正确加载，同时保持类加载器层次结构的完整性
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

    /**
     * 重写类加载方法，实现自定义的类加载策略
     * <p>
     * 该方法实现了以下加载策略：
     * 1. 首先检查类是否已被加载，如果已加载则直接返回
     * 2. 对于sun.和java.开头的系统类，优先从父类加载器加载，避免ClassNotFoundException
     * 3. 尝试从当前类加载器加载类
     * 4. 如果当前类加载器加载失败，则委托给父类加载器
     * </p>
     *
     * @param name 要加载的类的完全限定名
     * @param resolve 如果为true，则在加载后解析该类
     * @return 加载的Class对象
     * @throws ClassNotFoundException 如果类无法被找到
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 检查类是否已经被加载过
        // findLoadedClass是ClassLoader的方法，用于查找已加载的类
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            // 类已加载，直接返回
            return loadedClass;
        }

        // 优先从parent（SystemClassLoader）里加载系统类，避免抛出ClassNotFoundException
        // 这种策略可以确保系统类（如java.lang.String等）由正确的类加载器加载
        // 避免了因类加载器隔离导致的类型不匹配问题
        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }

        // 尝试从当前类加载器的路径中查找并加载类
        // findClass方法会根据URL数组中的路径查找类文件
        try {
            Class<?> aClass = findClass(name);
            // 如果需要解析类，则进行解析
            // 类解析包括初始化类的静态变量、执行静态代码块等
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            // 加载失败，忽略异常，继续尝试父类加载器
            // 这里使用try-catch而不是throws，是为了实现类加载的回退机制
            // ignore
        }

        // 如果当前类加载器无法加载，则委托给父类加载器
        // 这是双亲委派模型的标准做法
        return super.loadClass(name, resolve);
    }
}
