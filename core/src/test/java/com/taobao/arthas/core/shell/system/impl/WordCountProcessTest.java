package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.command.model.HelpModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.term.Tty;
import io.termd.core.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

public class WordCountProcessTest {

    @Test
    public void testHelpFlagsDisplayHelpWithoutExecutingTheCommand() {
        Command command = new InternalCommandManager(new BuiltinCommandPack(Collections.<String>emptyList()))
                .getCommand("wc");
        Assert.assertNotNull(command);

        for (String option : new String[] { "-h", "--help" }) {
            Tty tty = Mockito.mock(Tty.class);
            ResultDistributor distributor = Mockito.mock(ResultDistributor.class);
            Handler<CommandProcess> handler = new Handler<CommandProcess>() {
                @Override
                public void handle(CommandProcess process) {
                    Assert.fail("Help must not execute the pipe-only command");
                }
            };
            ProcessImpl process = new ProcessImpl(command, CliTokens.tokenize(option), handler,
                    new ProcessImpl.ProcessOutput(Collections.<Function<String, String>>emptyList(), null, tty),
                    distributor);
            process.setTty(tty);

            process.run();

            ArgumentCaptor<ResultModel> result = ArgumentCaptor.forClass(ResultModel.class);
            Mockito.verify(distributor, Mockito.times(2)).appendResult(result.capture());
            Assert.assertTrue(result.getAllValues().get(0) instanceof HelpModel);
            HelpModel help = (HelpModel) result.getAllValues().get(0);
            Assert.assertEquals("wc", help.getDetailCommand().getName());
            Assert.assertTrue(help.getDetailCommand().getUsage().contains("-l"));
            Assert.assertTrue(result.getAllValues().get(1) instanceof StatusModel);
            Assert.assertEquals(ExecStatus.TERMINATED, process.status());
        }
    }
}
