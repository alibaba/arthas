package com.taobao.arthas.core.command.express;

import java.lang.ref.WeakReference;

/**
 * 表达式工厂类
 * 负责创建和管理表达式对象，支持线程池化和非池化两种方式
 *
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {

    /**
     * 线程局部变量，存储表达式对象的弱引用
     *
     * 这里不能直接在 ThreadLocalMap 里强引用 Express（它由 ArthasClassLoader 加载），否则 stop/detach 后会被业务线程持有，
     * 导致 ArthasClassLoader 无法被 GC 回收。
     *
     * 用 WeakReference 打断强引用链：Thread -> ThreadLocalMap -> value(WeakReference) -X-> Express。
     * 这样当 Express 对象没有其他强引用时，可以被垃圾回收器回收，避免内存泄漏
     */
    private static final ThreadLocal<WeakReference<Express>> expressRef = ThreadLocal
            .withInitial(() -> new WeakReference<Express>(new OgnlExpress()));

    /**
     * 获取线程局部变量中的表达式对象
     * 该方法会复用线程中的表达式对象，提高性能
     *
     * @param object 要绑定的对象，表达式的上下文对象
     * @return 重置并绑定后的表达式对象
     */
    public static Express threadLocalExpress(Object object) {
        // 从 ThreadLocal 中获取表达式对象的弱引用
        WeakReference<Express> reference = expressRef.get();
        // 从弱引用中获取实际的表达式对象
        Express express = reference == null ? null : reference.get();
        // 如果表达式对象已被回收（弱引用返回null），则创建新的对象
        if (express == null) {
            express = new OgnlExpress();
            // 将新创建的表达式对象包装成弱引用并存入 ThreadLocal
            expressRef.set(new WeakReference<Express>(express));
        }
        // 重置表达式状态并绑定到指定的对象，然后返回
        return express.reset().bind(object);
    }

    /**
     * 创建非池化的表达式对象
     * 每次调用都会创建新的表达式对象，不会被线程缓存
     *
     * @param classloader 类加载器，用于加载表达式执行时需要的类
     *                   如果为 null，则使用系统类加载器
     * @return 新创建的表达式对象
     */
    public static Express unpooledExpress(ClassLoader classloader) {
        // 如果类加载器为 null，使用系统类加载器
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        // 使用指定的类加载器创建新的表达式对象
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }
}
