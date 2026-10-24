package com.taobao.arthas.core.shell.impl;

import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.command.basic1000.HelpCommand;
import com.taobao.arthas.core.command.basic1000.WordCountCommand;
import com.taobao.arthas.core.command.model.CommandOptionVO;
import com.taobao.arthas.core.command.model.CommandVO;
import com.taobao.arthas.core.command.model.HelpModel;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WordCountCommandTest {

    private List<CommandResolver> resolvers;
    private Command command;

    @Before
    public void setUp() {
        resolvers = Arrays.<CommandResolver>asList(new BuiltinCommandResolver(),
                new BuiltinCommandPack(Collections.<String>emptyList()));
        command = new InternalCommandManager(resolvers).getCommand("wc");
        Assert.assertNotNull(command);
    }

    @Test
    public void testHelpListsWcOnce() {
        HelpModel help = help(null);
        int count = 0;
        for (CommandVO entry : help.getCommands()) {
            if ("wc".equals(entry.getName())) {
                Assert.assertNotNull(entry.getSummary());
                count++;
            }
        }
        Assert.assertEquals(1, count);
    }

    @Test
    public void testHelpWcDescribesOptionsAndExamples() {
        CommandVO detail = help("wc").getDetailCommand();

        Assert.assertEquals("wc", detail.getName());
        Assert.assertNotNull(detail.cli());
        Assert.assertTrue(detail.getDescription().contains("thread | wc -l"));
        Assert.assertTrue(detail.getDescription().contains("https://arthas.aliyun.com/doc/wc"));
        Assert.assertTrue(detail.getUsage().contains("-l"));
        Assert.assertTrue(detail.getUsage().contains("-h"));
        boolean linesFound = false;
        boolean helpFound = false;
        for (CommandOptionVO option : detail.getOptions()) {
            if ("l".equals(option.getShortName())) {
                Assert.assertEquals("lines", option.getLongName());
                linesFound = true;
            }
            if ("h".equals(option.getShortName())) {
                Assert.assertEquals("help", option.getLongName());
                helpFound = true;
            }
        }
        Assert.assertTrue(linesFound);
        Assert.assertTrue(helpFound);
    }

    @Test
    public void testLineOptionsUseTheSameCommandDefinition() {
        for (String option : new String[] { "-l", "--lines" }) {
            WordCountCommand instance = new WordCountCommand();
            CommandLine commandLine = command.cli().parse(Collections.singletonList(option));
            CLIConfigurator.inject(commandLine, instance);
            Assert.assertTrue(instance.isLineMode());
        }
    }

    @Test
    public void testStandaloneCommandExplainsPipeUsage() {
        CommandProcess process = Mockito.mock(CommandProcess.class);

        new WordCountCommand().process(process);

        Mockito.verify(process).end(-1, "The wc command is only for pipes. See 'wc --help'\n");
    }

    private HelpModel help(String name) {
        Session session = Mockito.mock(Session.class);
        Mockito.when(session.getCommandResolvers()).thenReturn(resolvers);
        CommandProcess process = Mockito.mock(CommandProcess.class);
        Mockito.when(process.session()).thenReturn(session);
        HelpCommand help = new HelpCommand();
        help.setCmd(name);

        help.process(process);

        ArgumentCaptor<HelpModel> result = ArgumentCaptor.forClass(HelpModel.class);
        Mockito.verify(process).appendResult(result.capture());
        Mockito.verify(process).end();
        return result.getValue();
    }
}
