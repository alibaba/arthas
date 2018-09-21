package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author earayu
 */
public class ArthasCheckUtilsTest {

    @Test
    public void testIsIn(){
        Assert.assertTrue(ArthasCheckUtils.isIn(1,1,2,3));
        Assert.assertFalse(ArthasCheckUtils.isIn(1,2,3,4));
        Assert.assertTrue(ArthasCheckUtils.isIn(null,1,null,2));
        Assert.assertFalse(ArthasCheckUtils.isIn(1,null));
        Assert.assertTrue(ArthasCheckUtils.isIn(1L,1L,2L,3L));
        Assert.assertFalse(ArthasCheckUtils.isIn(1L,2L,3L,4L));
        Assert.assertTrue(ArthasCheckUtils.isIn("foo","foo","bar"));
        Assert.assertFalse(ArthasCheckUtils.isIn("foo","bar","goo"));
    }


    @Test
    public void testIsEquals(){
        Assert.assertTrue(ArthasCheckUtils.isEquals(1,1));
        Assert.assertTrue(ArthasCheckUtils.isEquals(1L,1L));
        Assert.assertTrue(ArthasCheckUtils.isEquals("foo","foo"));
        Assert.assertFalse(ArthasCheckUtils.isEquals(1,2));
        Assert.assertFalse(ArthasCheckUtils.isEquals("foo","bar"));
    }
}
