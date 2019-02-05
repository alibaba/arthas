package com.taobao.arthas.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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

        dynamicCompiler.addSource("TestLogger2", IOUtils.toString(logger2Stream, Charset.defaultCharset()));
        dynamicCompiler.addSource("TestLogger1", IOUtils.toString(logger1Stream, Charset.defaultCharset()));

        Map<String, byte[]> byteCodes = dynamicCompiler.buildByteCodes();

        Assert.assertTrue("TestLogger1", byteCodes.containsKey("com.test.TestLogger1"));
        Assert.assertTrue("TestLogger2", byteCodes.containsKey("com.hello.TestLogger2"));
    }

}
