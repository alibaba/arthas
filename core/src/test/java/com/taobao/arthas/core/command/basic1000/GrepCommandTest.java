package com.taobao.arthas.core.command.basic1000;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

/**
 * 
 * @author hengyunabc 2019-10-31
 *
 */
public class GrepCommandTest {

    private static CLI cli = null;

    @Before
    public void before() {
        cli = CLIConfigurator.define(GrepCommand.class, true);
    }

    @Test
    public void test() {
        List<String> args = Arrays.asList("-v", "ppp");
        GrepCommand grepCommand = new GrepCommand();
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(grepCommand.isInvertMatch());
    }

    @Test
    public void test2() {
        List<String> args = Arrays.asList("--before-context=6", "ppp");
        GrepCommand grepCommand = new GrepCommand();
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(6, grepCommand.getBeforeLines());
    }

    @Test
    public void test3() {
        List<String> args = Arrays.asList("--trim-end=false", "ppp");
        GrepCommand grepCommand = new GrepCommand();
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertFalse(grepCommand.isTrimEnd());
    }

    @Test
    public void testCount() {
        List<String> args = Arrays.asList("-c", "HttpClient");
        GrepCommand grepCommand = new GrepCommand();
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(grepCommand.isCount());
        Assert.assertEquals("HttpClient", grepCommand.getPattern());
    }

    @Test
    public void testContext() {
        List<String> args = Arrays.asList("-C", "2", "HttpClient");
        GrepCommand grepCommand = new GrepCommand();
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertFalse(grepCommand.isCount());
        Assert.assertEquals(2, grepCommand.getContext());
        Assert.assertEquals("HttpClient", grepCommand.getPattern());
    }
}
