package com.taobao.arthas.common;


import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * 
 * @author hengyunabc 2023-09-21
 *
 */
public class UnsafeUtils {
    public static final Unsafe UNSAFE;
    private static MethodHandles.Lookup IMPL_LOOKUP;

    static {
        Unsafe unsafe = null;
        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = (Unsafe) theUnsafeField.get(null);
        } catch (Throwable ignored) {
            // ignored
        }
        UNSAFE = unsafe;
    }

    public static MethodHandles.Lookup implLookup() {
        if (IMPL_LOOKUP == null) {
            Class<MethodHandles.Lookup> lookupClass = MethodHandles.Lookup.class;

            try {
                Field implLookupField = lookupClass.getDeclaredField("IMPL_LOOKUP");
                long offset = UNSAFE.staticFieldOffset(implLookupField);
                IMPL_LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(implLookupField), offset);
            } catch (Throwable e) {
                // ignored
            }
        }
        return IMPL_LOOKUP;
    }
}
