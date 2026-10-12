package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliTokens;
import org.junit.Assert;
import org.junit.Test;

public class WordCountHandlerTest {

    @Test
    public void testShortAndLongOptionsCountLines() {
        for (String option : new String[] { "-l", "--lines" }) {
            StdoutHandler handler = StdoutHandler.inject(CliTokens.tokenize("wc " + option));

            Assert.assertTrue(handler instanceof StatisticsFunction);
            Assert.assertNull(handler.apply("first\nsecond\n"));
            Assert.assertNull(handler.apply("third\n"));
            Assert.assertEquals("3\n", ((StatisticsFunction) handler).result());
        }
    }

    @Test
    public void testEmptyInput() {
        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));

        Assert.assertEquals("0\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testLineModeMustBeRequested() {
        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc"));

        Assert.assertNull(handler.apply("first\n"));
        Assert.assertEquals("wc currently only support wc -l!\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testCountersAreIndependent() {
        StdoutHandler first = WordCountHandler.inject(CliTokens.tokenize("wc -l"));
        StdoutHandler second = WordCountHandler.inject(CliTokens.tokenize("wc --lines"));

        first.apply("first\nsecond\n");
        second.apply("third\n");

        Assert.assertEquals("2\n", ((StatisticsFunction) first).result());
        Assert.assertEquals("1\n", ((StatisticsFunction) second).result());
    }
}
