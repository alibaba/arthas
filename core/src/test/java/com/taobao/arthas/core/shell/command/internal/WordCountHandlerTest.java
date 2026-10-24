package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliTokens;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

        Assert.assertNull(handler.apply(null));
        Assert.assertNull(handler.apply(""));
        Assert.assertEquals("0\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testCountsBlankAndWhitespaceOnlyLines() {
        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));

        handler.apply("\n");
        handler.apply(" \n\t\n");

        Assert.assertEquals("3\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testCountsTrailingBlankLines() {
        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));

        handler.apply("first\n\n\n");

        Assert.assertEquals("3\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testDoesNotCountUnterminatedText() {
        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));

        handler.apply("first");
        Assert.assertEquals("0\n", ((StatisticsFunction) handler).result());
        handler.apply("\nsecond");
        Assert.assertEquals("1\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testCountDoesNotDependOnWriteBoundaries() {
        String output = "first\r\n\nsecond\nlast";
        for (int split = 0; split <= output.length(); split++) {
            StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc --lines"));
            handler.apply(output.substring(0, split));
            handler.apply(output.substring(split));

            Assert.assertEquals("split at " + split, "3\n", ((StatisticsFunction) handler).result());
        }

        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));
        for (int i = 0; i < output.length(); i++) {
            handler.apply(output.substring(i, i + 1));
        }
        Assert.assertEquals("3\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testCarriageReturnDoesNotAddALine() {
        StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));

        handler.apply("first\rsecond\r");

        Assert.assertEquals("0\n", ((StatisticsFunction) handler).result());
    }

    @Test
    public void testConcurrentWritesPreserveTheTotal() throws Exception {
        final StdoutHandler handler = WordCountHandler.inject(CliTokens.tokenize("wc -l"));
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Void>> writes = new ArrayList<Future<Void>>();
            for (int thread = 0; thread < 4; thread++) {
                writes.add(executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        for (int i = 0; i < 1000; i++) {
                            handler.apply("first\n\n");
                        }
                        return null;
                    }
                }));
            }
            for (Future<Void> write : writes) {
                write.get(5, TimeUnit.SECONDS);
            }
            Assert.assertEquals("8000\n", ((StatisticsFunction) handler).result());
        } finally {
            executor.shutdownNow();
        }
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
