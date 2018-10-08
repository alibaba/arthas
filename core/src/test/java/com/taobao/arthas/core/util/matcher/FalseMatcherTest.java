package com.taobao.arthas.core.util.matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author earayu
 */
public class FalseMatcherTest {

    @Test
    public void testMatching(){
        Assert.assertFalse(new FalseMatcher<String>().matching(null));
        Assert.assertFalse(new FalseMatcher<Integer>().matching(1));
        Assert.assertFalse(new FalseMatcher<String>().matching(""));
        Assert.assertFalse(new FalseMatcher<String>().matching("foobar"));
    }

}
