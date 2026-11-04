package com.taobao.arthas.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IPUtilsTest {
    
    @Test
    public void testZeroIPv4() {
        String zero = "0.0.0.0";
        assertEquals(true, IPUtils.isAllZeroIP(zero));
    }

    @Test
    public void testZeroIPv6() {
        String zero = "::";
        assertEquals(true, IPUtils.isAllZeroIP(zero));
    }

    @Test
    public void testNormalIPv6() {
        String ipv6 = "2001:db8:85a3::8a2e:370:7334";
        assertEquals(false, IPUtils.isAllZeroIP(ipv6));
    }

    @Test
    public void testLeadingZerosIPv6() {
        String ipv6 = "0000::0000:0000";
        assertEquals(true, IPUtils.isAllZeroIP(ipv6));
    }

    @Test
    public void testTrailingZerosIPv6() {
        String ipv6 = "::0000:0000:0000";
        assertEquals(true, IPUtils.isAllZeroIP(ipv6));
    }

    @Test
    public void testMixedZerosIPv6() {
        String ipv6 = "0000::0000:0000:0000:0000";
        assertEquals(true, IPUtils.isAllZeroIP(ipv6));
    }

    @Test
    public void testEmptyIPv6() {
        String empty = "";
        assertEquals(false, IPUtils.isAllZeroIP(empty));
    }

    @Test
    public void testBlankIPv6() {
        String blank = " ";
        assertEquals(false, IPUtils.isAllZeroIP(blank));
    }
}
