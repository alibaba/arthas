package com.taobao.arthas.core.command.express;

import java.lang.ref.WeakReference;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ExpressTypeEnum;

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
    private static final ThreadLocal<WeakReference<Express>> expressRefQLExpress = ThreadLocal
            .withInitial(() -> new WeakReference<Express>(new QLExpress()));

    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static Express threadLocalExpress(Object object) {
        if (GlobalOptions.ExpressType.equals(ExpressTypeEnum.QLEXPRESS.getExpressType())) {
            return getOrCreateExpress(expressRefQLExpress, QLExpress::new).reset().bind(object);
        }
        return getOrCreateExpress(expressRef, OgnlExpress::new).reset().bind(object);
    }

    /**
     * 从 WeakReference 中获取 Express，如果已被 GC 回收则重新创建
     */
    private static Express getOrCreateExpress(ThreadLocal<WeakReference<Express>> threadLocal,
            java.util.function.Supplier<Express> supplier) {
        WeakReference<Express> reference = threadLocal.get();
        Express express = reference == null ? null : reference.get();
        if (express == null) {
            express = supplier.get();
            threadLocal.set(new WeakReference<Express>(express));
        }
        return express;
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        if (GlobalOptions.ExpressType.equals(ExpressTypeEnum.QLEXPRESS.getExpressType())) {
            return new QLExpress(new QLExpressClassLoaderClassResolver(classloader));
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }


    public static Express unpooledExpressByQL(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new QLExpress(new QLExpressClassLoaderClassResolver(classloader));
    }

    public static Express unpooledExpressByOGNL(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }
}
