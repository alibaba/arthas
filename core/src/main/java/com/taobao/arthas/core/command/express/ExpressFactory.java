package com.taobao.arthas.core.command.express;

import java.lang.ref.WeakReference;

/**
 * ExpressFactory
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {

    /**
     * 这里不能直接在 ThreadLocalMap 里强引用 Express（它由 ArthasClassLoader 加载），否则 stop/detach 后会被业务线程持有，
     * 导致 ArthasClassLoader 无法被 GC 回收。
     *
     * 用 WeakReference 打断强引用链：Thread -> ThreadLocalMap -> value(WeakReference) -X-> Express。
     */
    private static final ThreadLocal<WeakReference<Express>> expressRef = ThreadLocal
            .withInitial(() -> new WeakReference<Express>(new OgnlExpress()));

    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static Express threadLocalExpress(Object object) {
        WeakReference<Express> reference = expressRef.get();
        Express express = reference == null ? null : reference.get();
        if (express == null) {
            express = new OgnlExpress();
            expressRef.set(new WeakReference<Express>(express));
        }
        return express.reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }
}
