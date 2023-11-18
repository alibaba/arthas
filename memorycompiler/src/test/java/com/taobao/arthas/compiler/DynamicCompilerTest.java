package com.taobao.arthas.compiler;

import com.taobao.arthas.common.ReflectUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author hengyunabc 2019-02-06
 */

public class DynamicCompilerTest {

    @Test
    public void test() throws IOException, ClassNotFoundException {
        DynamicCompiler dynamicCompiler = new DynamicCompiler();
        InputStream logger1Stream = DynamicCompilerTest.class.getClassLoader().getResourceAsStream("TestLogger1.java");
        InputStream logger2Stream = DynamicCompilerTest.class.getClassLoader().getResourceAsStream("TestLogger2.java");

        dynamicCompiler.addSource("TestLogger2", toString(logger2Stream));
        dynamicCompiler.addSource("TestLogger1", toString(logger1Stream));

        Map<String, byte[]> byteCodes = dynamicCompiler.buildByteCodes();

        Assert.assertTrue("arthas.test.TestLogger1", byteCodes.containsKey("arthas.test.TestLogger1"));
        Assert.assertTrue("arthas.test.TestLogger2", byteCodes.containsKey("arthas.test.TestLogger2"));
    }

    @Test
    public void testLombokCompile() throws IOException, ClassNotFoundException {
        DynamicCompiler dynamicCompiler = new DynamicCompiler();
        dynamicCompiler.addSource("TestLombok", toString(DynamicCompilerTest.class.getClassLoader().getResourceAsStream("TestLombok.java")));
        dynamicCompiler.addProcessor("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
        dynamicCompiler.addProcessor("lombok.launch.AnnotationProcessorHider$ClaimingProcessor");

        Map<String, byte[]> byteCodes = dynamicCompiler.buildByteCodes();
        byteCodes.forEach((k, v) -> {
            loadClass(k, v);
        });
        Class<?> clazz = Class.forName("arthas.test.TestLombok");
        Method[] declaredMethods = clazz.getDeclaredMethods();
        out:
        for (Field field : clazz.getDeclaredFields()) {
            for (Method declaredMethod : declaredMethods) {
                if (declaredMethod.getName().equalsIgnoreCase("set" + field.getName()) ||
                        declaredMethod.getName().equalsIgnoreCase("get" + field.getName())) {
                    continue out;
                }
            }
            throw new RuntimeException("No lombok annotation processor");
        }
    }

    /**
     * Get the contents of an <code>InputStream</code> as a String
     * using the default character encoding of the platform.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested String
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    public static String toString(InputStream input) throws IOException {
        BufferedReader br = null;
        try {
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static void loadClass(String className, byte[] classData) {
        ClassLoader classLoader = DynamicCompilerTest.class.getClassLoader();
        try {
            ReflectUtils.defineClass(className, classData, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
