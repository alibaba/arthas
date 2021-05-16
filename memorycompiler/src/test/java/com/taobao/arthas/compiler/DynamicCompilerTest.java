package com.taobao.arthas.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hengyunabc 2019-02-06
 *
 */
public class DynamicCompilerTest {

    @Test
    public void test() throws IOException {
        String jarPath = LoggerFactory.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        File file = new File(jarPath);

        URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() },
                        ClassLoader.getSystemClassLoader().getParent());

        DynamicCompiler dynamicCompiler = new DynamicCompiler(classLoader);

        InputStream logger1Stream = DynamicCompilerTest.class.getClassLoader().getResourceAsStream("TestLogger1.java");
        InputStream logger2Stream = DynamicCompilerTest.class.getClassLoader().getResourceAsStream("TestLogger2.java");

        dynamicCompiler.addSource("TestLogger2", toString(logger2Stream));
        dynamicCompiler.addSource("TestLogger1", toString(logger1Stream));

        Map<String, byte[]> byteCodes = dynamicCompiler.buildByteCodes();

        Assert.assertTrue("TestLogger1", byteCodes.containsKey("com.test.TestLogger1"));
        Assert.assertTrue("TestLogger2", byteCodes.containsKey("com.hello.TestLogger2"));
    }

    /**
     * Get the contents of an <code>InputStream</code> as a String
     * using the default character encoding of the platform.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input  the <code>InputStream</code> to read from
     * @return the requested String
     * @throws NullPointerException if the input is null
     * @throws IOException if an I/O error occurs
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
}
