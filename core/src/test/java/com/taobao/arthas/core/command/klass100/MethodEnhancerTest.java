package com.taobao.arthas.core.command.klass100;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.taobao.arthas.compiler.DynamicCompiler;
import com.taobao.arthas.core.command.klass100.RunScriptCommand.MethodEnhancer;
import com.taobao.arthas.core.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.junit.Assert.fail;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ASM7;

/**
 * @author bucong
 * @date 2019/10/30
 */
public class MethodEnhancerTest {

    private String srcFile;
    
    private File dumpDir;

    @Before
    public void before() {
        //srcFile = "/Users/bucong/study/arthas/core/src/test/resource/T1.java";
        srcFile = MethodEnhancerTest.class.getResource("/T1.java").getPath();
        dumpDir = new File(srcFile).getParentFile();
    }

    @Test
    public void testRs() {
        try {
            DynamicCompiler dynamicCompiler = getDynamicCompiler(Collections.singletonList(srcFile));
            final TreeMap<String, byte[]> bytesMap = new TreeMap<String, byte[]>(dynamicCompiler.buildByteCodes());

            ClassLoader runClassloader = new ClassLoader(MethodEnhancer.class.getClassLoader()) {

                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    byte[] bytes = bytesMap.get(name);
                    if (bytes != null) {
                        return super.defineClass(name, bytes, 0, bytes.length);
                    }
                    return super.loadClass(name);
                }
            };

            enhance(bytesMap);

            Class<?> clazz = Class.forName(bytesMap.firstEntry().getKey(), true, runClassloader);
            Method meth = clazz.getMethod("main", String[].class);
            meth.invoke(null, new String[1]);

            writeModified(bytesMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private void enhance(TreeMap<String, byte[]> bytesMap) throws Exception {
        for (Entry<String, byte[]> entry : bytesMap.entrySet()) {
            ClassReader classReader = new ClassReader(entry.getValue());
            ClassWriter cw = new ClassWriter(classReader, COMPUTE_FRAMES | COMPUTE_MAXS);
            ClassVisitor cv = new ClassVisitor(ASM7, cw) {

                @Override
                public MethodVisitor visitMethod(int access, final String name, final String desc,
                                                 final String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodEnhancer(CommandPrint.class, access, name, desc, signature, exceptions, mv);
                }
            };
            classReader.accept(cv, ClassReader.EXPAND_FRAMES);
            entry.setValue(cw.toByteArray());
        }
    }

    private DynamicCompiler getDynamicCompiler(List<String> sourcefiles) throws IOException {
        DynamicCompiler dynamicCompiler = new DynamicCompiler(MethodEnhancer.class.getClassLoader());
        Charset charset = Charset.defaultCharset();
        if ("UTF-8" != null) {
            charset = Charset.forName("UTF-8");
        }
        for (String sourceFile : sourcefiles) {
            String sourceCode = FileUtils.readFileToString(new File(sourceFile), charset);
            String name = new File(sourceFile).getName();
            if (name.endsWith(".java")) {
                name = name.substring(0, name.length() - ".java".length());
            }
            dynamicCompiler.addSource(name, sourceCode);
        }
        return dynamicCompiler;
    }

    public static class CommandPrint {

        public static Method printlnMethod;

        public static Method printMethod;

        static {
            try {
                printlnMethod = CommandPrint.class.getDeclaredMethod("println", String.class);
                printMethod = CommandPrint.class.getDeclaredMethod("print", String.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        public static void println(String s) {
            System.out.println("hello::: " + s);
        }

        public static void print(String s) {
            System.out.print("hello>>> " + s);
        }
    }

    private void writeModified(Map<String, byte[]> map) throws IOException {
        for (Entry<String, byte[]> entry : map.entrySet()) {
            String clazzName = entry.getKey();
            File outfile = new File(dumpDir, clazzName.substring(clazzName.lastIndexOf(".") + 1) + ".class");
            FileUtils.writeByteArrayToFile(outfile, entry.getValue());
        }
        System.out.println("=============================================");
        System.out.println("dump dir:\n" + dumpDir);
        System.out.println("=============================================");
    }
}
