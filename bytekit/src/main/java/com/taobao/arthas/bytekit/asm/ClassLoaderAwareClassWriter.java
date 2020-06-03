package com.taobao.arthas.bytekit.asm;

import com.alibaba.arthas.deps.org.objectweb.asm.ClassReader;
import com.alibaba.arthas.deps.org.objectweb.asm.ClassWriter;

/**
 * @author vlinux
 * @author hengyunabc 2020-05-29
 *
 */
public class ClassLoaderAwareClassWriter extends ClassWriter {
    private ClassLoader classLoader;

    public ClassLoaderAwareClassWriter(int flags, ClassLoader loader) {
        this(null, flags, loader);
    }

    public ClassLoaderAwareClassWriter(ClassReader classReader, int flags, ClassLoader loader) {
        super(classReader, flags);
        this.classLoader = loader;
    }

    /*
     * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
     * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
     * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
     * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
     *
     * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
     * 规避了原有代码采用Object.class.getClassLoader()的方式
     */
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        Class<?> c, d;
        try {
            c = Class.forName(type1.replace('/', '.'), false, classLoader);
            d = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }
        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }
}
