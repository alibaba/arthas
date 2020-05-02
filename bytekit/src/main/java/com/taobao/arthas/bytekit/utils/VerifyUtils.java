package com.taobao.arthas.bytekit.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import com.alibaba.arthas.deps.org.objectweb.asm.ClassReader;
import com.alibaba.arthas.deps.org.objectweb.asm.ClassVisitor;
import com.alibaba.arthas.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.util.CheckClassAdapter;

/**
 *
 * @author hengyunabc
 *
 */
public class VerifyUtils {

    public static void asmVerify(byte[] bytes, boolean printResults) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ClassReader cr = new ClassReader(inputStream);
        CheckClassAdapter.verify(cr, true, new PrintWriter(System.out));
    }
    
    public static void asmVerify(byte[] bytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new CheckClassAdapter(cw);

        cr.accept(cv, 0);
    }

    public static Object instanceVerity(byte[] bytes) throws Exception {
        String name = Type.getObjectType(AsmUtils.toClassNode(bytes).name).getClassName();

        @SuppressWarnings("resource")
        ClassbyteClassLoader cl = new ClassbyteClassLoader(ClassLoaderUtils.getUrls(ClassLoader.getSystemClassLoader()),
                        ClassLoader.getSystemClassLoader().getParent());

        cl.addClass(name, bytes);

        Class<?> loadClass = cl.loadClass(name);
        return loadClass.newInstance();
    }

    public static Object invoke(Object instance, String name, Object... args) throws Exception {
        Method[] methods = instance.getClass().getMethods();
        for (Method method : methods) {
            if (name.contentEquals(method.getName())) {
                return method.invoke(instance, args);
            }
        }
        throw new NoSuchMethodError("name: " + name);
    }

    public static class ClassbyteClassLoader extends URLClassLoader {
        public ClassbyteClassLoader(URL[] urls, ClassLoader cl) {
            super(urls, cl);
        }

        public Class<?> addClass(String name, byte[] bytes) throws ClassFormatError {
            Class<?> cl = defineClass(name, bytes, 0, bytes.length);
            resolveClass(cl);

            return cl;
        }
    }

}
