package com.taobao.arthas.core.shell.cli.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CliTokenImplTest {

    /**
     * supported:
     * <p>
     * case1:
     * thread| grep xxx
     * [thread|, grep, xxx] -> [thread, |, grep, xxx]
     * case:2
     * thread | grep xxx
     * [thread, |, grep, xxx] -> [thread, |, grep, xxx]
     * case3:
     * thread |grep xxx
     * [thread, |grep] -> [thread, |, grep, xxx]
     */
    @Test
    public void testSupportedPipeCharWithoutRegex() {
        String[] expectedTextTokenValue = new String[]{"thread", "|", "grep", "xxx"};
        String cmd = "thread| grep xxx";
        List<CliToken> actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread | grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread |grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread'|' grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread '|' grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread '|'grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread\"|\" grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread \"|\" grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread \"|\"grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"thread| grep", "xxx"};
        cmd = "thread'| 'grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread\"| \"grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"thread |grep", "xxx"};
        cmd = "thread' |'grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "thread\" |\"grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"thread \"|\"grep", "xxx"};
        cmd = "thread' \"|\"'grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"thread '|'grep", "xxx"};
        cmd = "thread\" '|'\"grep xxx";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));


    }

    /**
     * supported:
     * <p>
     * case1:
     * trace -E classA|classB methodA|methodB| grep classA
     * [trace, -E, classA|classB, methodA|methodB|, grep, classA] -> [trace, -E, classA|classB, methodA|methodB, |, grep, classA]
     * case2:
     * trace -E classA|classB methodA|methodB | grep classA
     * [trace, -E, classA|classB, methodA|methodB, |, grep, classA] -> [trace, -E, classA|classB, methodA|methodB, |, grep, classA]
     * case3:
     * trace -E classA|classB methodA|methodB |grep classA
     * [trace, -E, classA|classB, methodA|methodB, |grep, classA] -> [trace, -E, classA|classB, methodA|methodB, |, grep, classA]
     */
    @Test
    public void testSupportedPipeCharWithRegex() {
        String[] expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB", "|", "grep", "classA"};
        String cmd = "trace -E classA|classB methodA|methodB| grep classA";
        List<CliToken> actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB | grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB |grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB'|' grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB '|' grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB '|'grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));


        cmd = "trace -E classA|classB methodA|methodB\"|\" grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB \"|\" grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB \"|\"grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB| grep", "classA"};
        cmd = "trace -E classA|classB methodA|methodB'| 'grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB\"| \"grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB |grep", "classA"};
        cmd = "trace -E classA|classB methodA|methodB' |'grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "trace -E classA|classB methodA|methodB\" |\"grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB '|'grep", "classA"};
        cmd = "trace -E classA|classB methodA|methodB\" '|'\"grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB \"|\"grep", "classA"};
        cmd = "trace -E classA|classB methodA|methodB' \"|\"'grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));
    }

    /**
     * unsupported:
     * <p>
     * case1:
     * thread|grep xxx
     * [thread|grep, xxx] -> [thread|grep, xxx]
     * case2:
     * trace -E classA|classB methodA|methodB|grep classA
     * [trace, -E, classA|classB, methodA|methodB|grep, classA] -> [trace, -E, classA|classB, methodA|methodB|grep, classA]
     * case3:
     * trace -E classA|classB| methodA|methodB | grep classA
     * [trace, -E, classA|classB|, methodA|methodB, |, grep, classA] -> [trace, -E, classA|classB，|, methodA|methodB, |, grep, classA]
     */
    @Test
    public void testUnSupportedPipeChar() {
        String[] expectedTextTokenValue = new String[]{"thread|grep", "xxx"};
        String cmd = "thread|grep xxx";
        List<CliToken> actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB|grep", "classA"};
        cmd = "trace -E classA|classB methodA|methodB|grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "|", "methodA|methodB", "|", "grep", "classA"};
        cmd = "trace -E classA|classB| methodA|methodB | grep classA";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));
    }

    @Test
    public void testSeparateRedirect() {
        String[] expectedTextTokenValue = new String[]{"jad", "aaa", ">", "bbb"};
        String cmd = "jad aaa> bbb";
        List<CliToken> actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa > bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa >bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa'>' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa '>' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa '>'bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa\">\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);

        cmd = "jad aaa \">\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa \">\"bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa >bbb"};

        cmd = "jad aaa' >'bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa\" >\"bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa> bbb"};

        cmd = "jad aaa\"> \"bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa'> 'bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa\\r", ">", "bbb"};

        cmd = "jad aaa'\\r>' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa\"\\r>\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa'", ">", "bbb"};
        cmd = "jad aaa\"'>\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa'>'", "bbb"};
        cmd = "jad aaa\"'>'\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa\">\"", "bbb"};
        cmd = "jad aaa'\">\"' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

    }

    @Test
    public void testSeparateRedirectAppend() {
        String[] expectedTextTokenValue = new String[]{"jad", "aaa", ">>", "bbb"};
        String cmd = "jad aaa>> bbb";
        List<CliToken> actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa >> bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa >>bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa'>>' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa '>>' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa '>>'bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa\">>\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);

        cmd = "jad aaa \">>\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa \">>\"bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa >>bbb"};

        cmd = "jad aaa' >>'bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa\" >>\"bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa>> bbb"};

        cmd = "jad aaa\">> \"bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa'>> 'bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa\\r", ">>", "bbb"};

        cmd = "jad aaa'\\r>>' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        cmd = "jad aaa\"\\r>>\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa'", ">>", "bbb"};
        cmd = "jad aaa\"'>>\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa'>>'", "bbb"};
        cmd = "jad aaa\"'>>'\" bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));

        expectedTextTokenValue = new String[]{"jad", "aaa\">>\"", "bbb"};
        cmd = "jad aaa'\">>\"' bbb";
        actualTokens = CliTokenImpl.tokenize(cmd);
        assertEqualsIgnoreBlank(expectedTextTokenValue, actualTokens);
        Assert.assertEquals(cmd, concatRaw(actualTokens));
    }

    private void assertEqualsIgnoreBlank(String[] expectedTextTokenValue, List<CliToken> actualTokens) {
        Assert.assertArrayEquals(expectedTextTokenValue, removeBlankToken(actualTokens));
    }

    private static String[] removeBlankToken(List<CliToken> cliTokens) {
        List<CliToken> copy = new ArrayList<>(cliTokens);
        return copy.stream()
                .filter(token -> !token.isBlank())
                .map(CliToken::value)
                .toArray(String[]::new);
    }

    private static String concatRaw(List<CliToken> tokens) {
        StringBuilder builder = new StringBuilder();
        for (CliToken token : tokens) {
            builder.append(token.raw());
        }
        return builder.toString();
    }

}
