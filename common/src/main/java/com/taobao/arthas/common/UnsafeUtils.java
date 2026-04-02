package com.taobao.arthas.common;


import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Unsafe工具类
 * 提供对JDK内部Unsafe类的访问，以及获取MethodHandles.Lookup的IMPL_LOOKUP实例
 * 用于在特殊场景下进行底层操作，如直接内存访问、方法注入等
 *
 * @author hengyunabc 2023-09-21
 *
 */
public class UnsafeUtils {
    /**
     * JDK内部的Unsafe实例
     * 提供底层的内存操作和对象操作能力
     */
    public static final Unsafe UNSAFE;

    /**
     * MethodHandles.Lookup的IMPL_LOOKUP实例
     * IMPL_LOOKUP提供了超越普通可见性限制的方法查找能力
     * 可以访问任意类的私有成员
     */
    private static MethodHandles.Lookup IMPL_LOOKUP;

    // 静态初始化块
    // 在类加载时获取Unsafe实例
    static {
        Unsafe unsafe = null;
        try {
            // 通过反射获取Unsafe类的theUnsafe字段
            // theUnsafe是Unsafe类中预先初始化的静态单例实例
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            // 设置字段可访问，绕过访问控制检查
            theUnsafeField.setAccessible(true);
            // 从静态字段中获取Unsafe实例
            unsafe = (Unsafe) theUnsafeField.get(null);
        } catch (Throwable ignored) {
            // 忽略所有异常，如果获取失败则unsafe保持为null
        }
        UNSAFE = unsafe;
    }

    /**
     * 获取MethodHandles.Lookup的IMPL_LOOKUP实例
     * IMPL_LOOKUP是Lookup的一个特殊实例，具有信任级别的访问权限
     * 可以创建对任何类（包括非public成员）的方法句柄
     *
     * @return IMPL_LOOKUP实例，如果获取失败则返回null
     */
    public static MethodHandles.Lookup implLookup() {
        // 延迟初始化，只在第一次调用时尝试获取
        if (IMPL_LOOKUP == null) {
            Class<MethodHandles.Lookup> lookupClass = MethodHandles.Lookup.class;

            try {
                // 获取Lookup类的IMPL_LOOKUP字段
                Field implLookupField = lookupClass.getDeclaredField("IMPL_LOOKUP");
                // 使用Unsafe获取静态字段的内存偏移量
                long offset = UNSAFE.staticFieldOffset(implLookupField);
                // 通过Unsafe直接读取静态字段的值
                // staticFieldBase获取字段所在的基对象
                // offset是字段的偏移量
                IMPL_LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(implLookupField), offset);
            } catch (Throwable e) {
                // 忽略所有异常，如果获取失败则IMPL_LOOKUP保持为null
            }
        }
        return IMPL_LOOKUP;
    }
}
