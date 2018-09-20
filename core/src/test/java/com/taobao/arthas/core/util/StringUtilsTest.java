package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author bohrqiu 2018-09-21 01:01
 */
public class StringUtilsTest {

    @Test
    public void testHumanReadableByteCount() {
        Assert.assertEquals(StringUtils.humanReadableByteCount(1023L), "1023 B");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L), "1.00 KiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024L), "1.00 MiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024L - 100), "1023.90 KiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024 * 1024L), "1.00 GiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024 * 1024 * 1024L), "1.00 TiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024 * 1024 * 1024 * 1024), "1.00 PiB");
    }
}