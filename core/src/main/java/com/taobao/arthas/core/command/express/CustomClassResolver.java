package com.taobao.arthas.core.command.express;

import ognl.ClassResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义类解析器
 *
 * 该类实现了OGNL的ClassResolver接口，用于在表达式解析过程中动态加载类。
 * 相比默认的类解析器，它提供了更灵活的类加载机制，支持上下文类加载器和自动添加java.lang包前缀。
 *
 * <p>主要特性：</p>
 * <ul>
 * <li>使用单例模式，全局共享一个实例</li>
 * <li>支持类加载缓存，提高性能</li>
 * <li>优先使用线程上下文类加载器加载类</li>
 * <li>自动为基本类型添加java.lang包前缀</li>
 * </ul>
 *
 * @author diecui1202 on 2017/9/29.
 * @see ognl.DefaultClassResolver
 */
public class CustomClassResolver implements ClassResolver {

    /**
     * 单例实例，全局共享的类解析器
     */
    public static final CustomClassResolver customClassResolver = new CustomClassResolver();

    /**
     * 类加载缓存，使用并发HashMap存储已加载的类
     * Key: 完整类名, Value: 对应的Class对象
     */
    private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(101);

    /**
     * 私有构造函数，防止外部创建实例
     * 保证单例模式的实现
     */
    private CustomClassResolver() {

    }

    /**
     * 根据类名加载对应的Class对象
     *
     * 该方法实现了类的动态加载，具有以下加载策略：
     * 1. 首先从缓存中查找，如果已加载则直接返回
     * 2. 缓存未命中时，使用线程上下文类加载器加载
     * 3. 如果上下文类加载器为空或加载失败，使用Class.forName()加载
     * 4. 如果类名中不包含包名（即不含'.'），自动添加java.lang前缀重试
     * 5. 加载成功后将类对象存入缓存并返回
     *
     * @param className 要加载的类名，可以是完整类名或简单类名
     * @param context OGNL表达式上下文（当前实现中未使用）
     * @return 加载完成的Class对象
     * @throws ClassNotFoundException 如果类无法被找到且无法通过添加java.lang前缀解决时抛出
     */
    @Override
    public Class classForName(String className, Map context) throws ClassNotFoundException {
        // 结果对象，用于存储加载的Class
        Class<?> result = null;

        // 尝试从缓存中获取已加载的类
        if ((result = classes.get(className)) == null) {
            try {
                // 获取当前线程的上下文类加载器
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

                // 如果上下文类加载器存在，使用它来加载类
                if (classLoader != null) {
                    result = classLoader.loadClass(className);
                } else {
                    // 否则使用默认的Class.forName()方法加载
                    result = Class.forName(className);
                }
            } catch (ClassNotFoundException ex) {
                // 如果加载失败，检查类名是否不包含包名（即不含'.'）
                // 如果是，尝试添加java.lang包前缀重新加载
                if (className.indexOf('.') == -1) {
                    result = Class.forName("java.lang." + className);
                    // 将添加了前缀的完整类名和对应的Class对象存入缓存
                    classes.put("java.lang." + className, result);
                }
            }
            // 将加载成功的类对象存入缓存，使用原始类名作为key
            classes.put(className, result);
        }
        return result;
    }
}
