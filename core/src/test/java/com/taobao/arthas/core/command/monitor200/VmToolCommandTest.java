package com.taobao.arthas.core.command.monitor200;

import org.junit.Assert;
import org.junit.Test;

public class VmToolCommandTest {

    @Test
    public void testNormalizeArrayClassNameNull() {
        Assert.assertNull(VmToolCommand.normalizeArrayClassName(null));
    }

    @Test
    public void testNormalizeArrayClassNameNonArrayReturnsInput() {
        Assert.assertEquals("java.lang.String", VmToolCommand.normalizeArrayClassName("java.lang.String"));
        Assert.assertEquals("demo.MathGame", VmToolCommand.normalizeArrayClassName("demo.MathGame"));
        Assert.assertEquals("", VmToolCommand.normalizeArrayClassName(""));
    }

    @Test
    public void testNormalizeArrayClassNameObjectArray() {
        Assert.assertEquals("[Ljava.lang.Object;",
                VmToolCommand.normalizeArrayClassName("java.lang.Object[]"));
    }

    @Test
    public void testNormalizeArrayClassNameStringMultiDimensional() {
        Assert.assertEquals("[[Ljava.lang.String;",
                VmToolCommand.normalizeArrayClassName("java.lang.String[][]"));
        Assert.assertEquals("[[[Ljava.lang.String;",
                VmToolCommand.normalizeArrayClassName("java.lang.String[][][]"));
    }

    @Test
    public void testNormalizeArrayClassNamePrimitiveArrays() {
        Assert.assertEquals("[Z", VmToolCommand.normalizeArrayClassName("boolean[]"));
        Assert.assertEquals("[B", VmToolCommand.normalizeArrayClassName("byte[]"));
        Assert.assertEquals("[C", VmToolCommand.normalizeArrayClassName("char[]"));
        Assert.assertEquals("[S", VmToolCommand.normalizeArrayClassName("short[]"));
        Assert.assertEquals("[I", VmToolCommand.normalizeArrayClassName("int[]"));
        Assert.assertEquals("[J", VmToolCommand.normalizeArrayClassName("long[]"));
        Assert.assertEquals("[F", VmToolCommand.normalizeArrayClassName("float[]"));
        Assert.assertEquals("[D", VmToolCommand.normalizeArrayClassName("double[]"));
    }

    @Test
    public void testNormalizeArrayClassNamePrimitiveMultiDimensional() {
        Assert.assertEquals("[[I", VmToolCommand.normalizeArrayClassName("int[][]"));
        Assert.assertEquals("[[[B", VmToolCommand.normalizeArrayClassName("byte[][][]"));
    }

    @Test
    public void testNormalizeArrayClassNameMatchesJvmInternal() {
        Assert.assertEquals(Object[].class.getName(),
                VmToolCommand.normalizeArrayClassName("java.lang.Object[]"));
        Assert.assertEquals(String[][].class.getName(),
                VmToolCommand.normalizeArrayClassName("java.lang.String[][]"));
        Assert.assertEquals(int[].class.getName(),
                VmToolCommand.normalizeArrayClassName("int[]"));
        Assert.assertEquals(long[][].class.getName(),
                VmToolCommand.normalizeArrayClassName("long[][]"));
    }

    @Test
    public void testNormalizeArrayClassNameJvmInternalFormatUnchanged() {
        Assert.assertEquals("[Ljava.lang.Object;",
                VmToolCommand.normalizeArrayClassName("[Ljava.lang.Object;"));
        Assert.assertEquals("[I", VmToolCommand.normalizeArrayClassName("[I"));
    }

    @Test
    public void testNormalizeArrayClassNameEmptyBaseReturnsInput() {
        Assert.assertEquals("[]", VmToolCommand.normalizeArrayClassName("[]"));
        Assert.assertEquals("[][]", VmToolCommand.normalizeArrayClassName("[][]"));
        Assert.assertEquals("[][][]", VmToolCommand.normalizeArrayClassName("[][][]"));
    }
}
