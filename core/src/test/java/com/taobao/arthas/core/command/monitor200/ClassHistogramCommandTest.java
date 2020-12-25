package com.taobao.arthas.core.command.monitor200;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author fornaix 2020-12-25
 *
 */
public class ClassHistogramCommandTest {

    private static class Dummy {
    }

    private Matcher classStatsMatcher;

    @Before
    public void init() {
        // skip tests when JRE version is lower than 1.8
        Assume.assumeTrue(isJRECompatible());

        classStatsMatcher = Pattern.compile("\\s*\\d+:\\s*(\\d+)\\s*\\d+\\s*(.*)\\s*")
                .matcher("");
    }

    @Test
    public void simpleTest() throws Exception {
        System.gc();
        Dummy dummy1 = new Dummy();
        Dummy dummy2 = new Dummy();
        Dummy[] dummyArray = new Dummy[2];
        String histogram = ClassHistogramCommand.run(false, -1);
        Assert.assertEquals(2, getNumInstance(histogram, Dummy.class));
        Assert.assertEquals(1, getNumInstance(histogram, Dummy[].class));
    }

    @Test
    public void testLive() throws Exception {
        System.gc();
        Dummy dummy = new Dummy();
        dummy = new Dummy();
        dummy = new Dummy();
        String histogram = ClassHistogramCommand.run(false, -1);
        Assert.assertEquals(3, getNumInstance(histogram, Dummy.class));
        histogram = ClassHistogramCommand.run(true, -1);
        Assert.assertEquals(1, getNumInstance(histogram, Dummy.class));
    }

    @Test
    public void testTruncate() throws Exception {
        String histogram = ClassHistogramCommand.run(false, 1);
        int lineNumber = histogram.split("\n").length;
        // expected: 3(header) + 1(class statistics) + 1(ellipsis) + 1(total statistics) = 6
        Assert.assertEquals(6, lineNumber);
    }

    private int getNumInstance(String histogram, Class<?> clazz) throws IOException {
        String className = clazz.getName();
        BufferedReader br = new BufferedReader(new StringReader(histogram));
        String line;
        while ((line = br.readLine()) != null) {
            classStatsMatcher.reset(line);
            if (classStatsMatcher.matches()) {
                if (className.equals(classStatsMatcher.group(2))) {
                    return Integer.parseInt(classStatsMatcher.group(1));
                }
            }
        }
        return -1;
    }

    private boolean isJRECompatible() {
        String version = System.getProperty("java.version");
        return version.startsWith("1.8") || !version.startsWith("1.");
    }
}
