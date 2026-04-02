package com.taobao.arthas.agent;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Arthas专用类加载器
 *
 * <p>这个类加载器实现了自定义的类加载策略，具有以下特点：
 * <ul>
 *   <li>继承URLClassLoader，支持从JAR文件加载类</li>
 *   <li>父类加载器设置为系统类加载器的父类（通常是扩展类加载器）</li>
 *   <li>实现了类加载的隔离，避免Arthas类与应用类冲突</li>
 *   <li>优先从自身加载类，只有系统类才委托给父加载器</li>
 *   <li>对于sun.*和java.*开头的系统类，优先从父加载器加载</li>
 * </ul>
 *
 * <p>这种设计实现了"子类优先"的加载策略（与双亲委派模型相反），
 * 确保Arthas使用自己的类版本，而不是应用中可能存在的同名类
 *
 * @author beiwei30 on 09/12/2016.
 */
public class ArthasClassloader extends URLClassLoader {
    /**
     * 构造Arthas类加载器
     *
     * @param urls 类加载器的类路径URL数组，通常包含arthas-core.jar的URL
     */
    public ArthasClassloader(URL[] urls) {
        // 调用父类URLClassLoader的构造函数
        // 父类加载器设置为系统类加载器的父类（通常是扩展类加载器或启动类加载器）
        // 这样可以确保系统核心类仍然由启动类加载器加载
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

    /**
     * 重写类加载方法，实现自定义的类加载策略
     *
     * <p>类加载顺序：
     * <ol>
     *   <li>检查类是否已加载，如果是则直接返回</li>
     *   <li>对于sun.*和java.*开头的系统类，委托给父加载器加载</li>
     *   <li>尝试从自身类路径（JAR文件）中加载类</li>
     *   <li>如果加载失败，委托给父加载器加载</li>
     * </ol>
     *
     * <p>这种策略确保：
     * <ul>
     *   <li>Arthas的类不会被应用中的同名类覆盖</li>
     *   <li>系统核心类仍然由正确的加载器加载</li>
     *   <li>实现了类加载隔离，避免冲突</li>
     * </ul>
     *
     * @param name 要加载的类的全限定名
     * @param resolve 如果为true，在加载后解析类
     * @return 加载的Class对象
     * @throws ClassNotFoundException 如果找不到类
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 第一步：检查类是否已经被加载
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            // 类已加载，直接返回
            return loadedClass;
        }

        // 第二步：优先从parent（SystemClassLoader）里加载系统类，避免抛出ClassNotFoundException
        // 对于系统核心类（sun.*和java.*），使用父加载器加载
        // 这样可以确保Java核心类库由启动类加载器加载，避免安全问题
        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }

        // 第三步：尝试从自身的类路径（arthas-core.jar）中加载类
        try {
            // 使用findClass在自身的类路径中查找并加载类
            Class<?> aClass = findClass(name);
            if (resolve) {
                // 如果需要解析类，则进行解析
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            // 在自身类路径中找不到类，继续尝试父加载器
            // ignore
        }

        // 第四步：委托给父加载器加载
        // 如果前面的步骤都失败，使用标准的双亲委派模型
        return super.loadClass(name, resolve);
    }
}
