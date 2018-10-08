package com.taobao.arthas.core.util.matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author earayu
 */
public class EqualsMatcherTest {

    @Test
    public void testMatching(){
        Assert.assertTrue(new EqualsMatcher<String>(null).matching(null));
        Assert.assertTrue(new EqualsMatcher<String>("").matching(""));
        Assert.assertTrue(new EqualsMatcher<String>("foobar").matching("foobar"));
        Assert.assertFalse(new EqualsMatcher<String>("").matching(null));
        Assert.assertFalse(new EqualsMatcher<String>("abc").matching("def"));
    }

}
