package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author gongdewei 2020/5/27
 */
public class HierarchyStringBuilderTest {

    @Test
    public void testConcatWithoutSeparator() {
        HierarchyStringBuilder sb = new HierarchyStringBuilder(null);
        Assert.assertEquals("ab", sb.concat("a", "b"));
        Assert.assertEquals("abc", sb.concat("a", "b", "c"));
        Assert.assertEquals("abcd", sb.concat("a", "b", "c", "d"));
    }

    @Test
    public void testConcatWithSeparator() {
        HierarchyStringBuilder sb = new HierarchyStringBuilder("/");
        Assert.assertEquals("a/b", sb.concat("a", "b"));
        Assert.assertEquals("a/b/c", sb.concat("a", "b", "c"));
        Assert.assertEquals("a/b/c/d", sb.concat("a", "b", "c", "d"));
    }


}
