package com.taobao.arthas.core.util.matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author earayu
 */
public class RegexMatcherTest {

    @Test
    public void testMatchingWithNullInputs(){
        Assert.assertFalse(new RegexMatcher(null).matching(null));
        Assert.assertFalse(new RegexMatcher(null).matching("foobar"));
        Assert.assertFalse(new RegexMatcher("foobar").matching(null));
        Assert.assertTrue(new RegexMatcher("foobar").matching("foobar"));
    }

    /**
     * test regux with . | * + ? \s \S \w \W and so on...
     */
    @Test
    public void testMatchingWithRegularGrammar(){
        Assert.assertTrue(new RegexMatcher("foo?").matching("fo"));
        Assert.assertTrue(new RegexMatcher("foo?").matching("foo"));
        Assert.assertTrue(new RegexMatcher("foo.").matching("fooo"));
        Assert.assertTrue(new RegexMatcher("foo*").matching("fooooo"));
        Assert.assertTrue(new RegexMatcher("foo.*").matching("foobarbarbar"));
        Assert.assertFalse(new RegexMatcher("foo+").matching("fo"));
        Assert.assertTrue(new RegexMatcher("foo+").matching("fooooo"));

        Assert.assertTrue(new RegexMatcher("foo\\s").matching("foo "));
        Assert.assertFalse(new RegexMatcher("foo\\S").matching("foo "));
        Assert.assertTrue(new RegexMatcher("foo\\w").matching("fooo"));
        Assert.assertTrue(new RegexMatcher("foo\\W").matching("foo "));
        Assert.assertFalse(new RegexMatcher("foo\\W").matching("fooo"));


        Assert.assertTrue(new RegexMatcher("foo[1234]").matching("foo1"));
        Assert.assertFalse(new RegexMatcher("foo[1234]").matching("foo5"));
        Assert.assertTrue(new RegexMatcher("foo\\\\").matching("foo\\"));
        Assert.assertTrue(new RegexMatcher("foo\\d").matching("foo5"));
        Assert.assertTrue(new RegexMatcher("fo{1,3}").matching("fo"));
        Assert.assertFalse(new RegexMatcher("fo{1,3}").matching("foooo"));
    }

    @Test
    public void testMatchingComplexRegex(){
        String ipAddressPattern = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";
        Assert.assertTrue(new RegexMatcher(ipAddressPattern).matching("1.1.1.1"));
        Assert.assertFalse(new RegexMatcher(ipAddressPattern).matching("255.256.255.0"));
        Assert.assertFalse(new RegexMatcher(ipAddressPattern).matching("1.1.1"));

        Assert.assertTrue(new RegexMatcher("^foobar$").matching("foobar"));
        Assert.assertFalse(new RegexMatcher("^foobar$").matching("\nfoobar"));
        Assert.assertFalse(new RegexMatcher("^foobar$").matching("foobar\n"));

        String emailAddressPattern = "[a-z\\d]+(\\.[a-z\\d]+)*@([\\da-z](-[\\da-z])?)+(\\.{1,2}[a-z]+)+";
        Assert.assertTrue(new RegexMatcher(emailAddressPattern).matching("foo@bar.com"));
        Assert.assertFalse(new RegexMatcher(emailAddressPattern).matching("asdfghjkl"));
    }

}
