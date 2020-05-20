package com.taobao.arthas.core.shell.cli.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
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
        List<CliToken> actualTokens = CliTokenImpl.tokenize("thread| grep xxx");
        assertEquals(expectedTextTokenValue, actualTokens);

        actualTokens = CliTokenImpl.tokenize("thread | grep xxx");
        assertEquals(expectedTextTokenValue, actualTokens);

        actualTokens = CliTokenImpl.tokenize("thread |grep xxx");
        assertEquals(expectedTextTokenValue, actualTokens);
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
        List<CliToken> actualTokens = CliTokenImpl.tokenize("trace -E classA|classB methodA|methodB| grep classA");
        assertEquals(expectedTextTokenValue, actualTokens);

        actualTokens = CliTokenImpl.tokenize("trace -E classA|classB methodA|methodB | grep classA");
        assertEquals(expectedTextTokenValue, actualTokens);

        actualTokens = CliTokenImpl.tokenize("trace -E classA|classB methodA|methodB |grep classA");
        assertEquals(expectedTextTokenValue, actualTokens);
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
        List<CliToken> actualTokens = CliTokenImpl.tokenize("thread|grep xxx");
        assertEquals(expectedTextTokenValue, actualTokens);

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "methodA|methodB|grep", "classA"};
        actualTokens = CliTokenImpl.tokenize("trace -E classA|classB methodA|methodB|grep classA");
        assertEquals(expectedTextTokenValue, actualTokens);

        expectedTextTokenValue = new String[]{"trace", "-E", "classA|classB", "|", "methodA|methodB", "|", "grep", "classA"};
        actualTokens = CliTokenImpl.tokenize("trace -E classA|classB| methodA|methodB | grep classA");
        assertEquals(expectedTextTokenValue, actualTokens);
    }

    private void assertEquals(String[] expectedTextTokenValue, List<CliToken> actualTokens) {
        removeBlankToken(actualTokens);
        for (int i = 0; i < expectedTextTokenValue.length; i++) {
            Assert.assertEquals(expectedTextTokenValue[i], actualTokens.get(i).value());
        }
    }

    private void removeBlankToken(List<CliToken> cliTokens) {
        CliToken blankToken = new CliTokenImpl(false, " ");
        Iterator<CliToken> it = cliTokens.iterator();
        while (it.hasNext()) {
            CliToken token = it.next();
            if (blankToken.equals(token)) {
                it.remove();
            }
        }
    }

}
