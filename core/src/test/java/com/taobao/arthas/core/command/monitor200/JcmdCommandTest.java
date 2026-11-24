package com.taobao.arthas.core.command.monitor200;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;

import javax.management.ObjectName;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JcmdCommandTest {

    @BeforeClass
    public static void requireDiagnosticCommandMBean() throws Exception {
        assumeTrue("DiagnosticCommand MBean is required", ManagementFactory.getPlatformMBeanServer()
                .isRegistered(new ObjectName("com.sun.management:type=DiagnosticCommand")));
    }

    @Test
    public void listAvailableCommandsByDefault() throws Throwable {
        assertThat(executeSuccessfully()).contains("VM.version", "Thread.print", "help");
    }

    @Test
    public void showHelpForDiagnosticCommand() throws Throwable {
        assertThat(executeSuccessfully("help", "VM.flags")).contains("VM.flags", "-all");
    }

    @Test
    public void invokeOperationWithoutParameters() throws Throwable {
        assertThat(executeSuccessfully("VM.version")).contains("JDK");
    }

    @Test
    public void invokeOperationWithEmptyArgumentArray() throws Throwable {
        assertThat(executeSuccessfully("Thread.print")).contains('"' + Thread.currentThread().getName() + '"');
    }

    @Test
    public void passDiagnosticOptionsAfterSeparator() throws Throwable {
        assertThat(executeSuccessfully("VM.flags", "--", "-all")).contains("MaxHeapSize");
    }

    @Test
    public void rejectUnknownCommand() throws Throwable {
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "not.a.command");

        verify(process).end(-1, "Unknown diagnostic command: not.a.command. Run 'jcmd' to list available commands.");
    }

    @Test
    public void rejectArgumentsForOperationWithoutParameters() throws Throwable {
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "VM.version", "unexpected");

        verify(process).end(-1, "Diagnostic command VM.version does not accept arguments.");
    }

    @Test
    public void reportDiagnosticArgumentErrors() throws Throwable {
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "VM.flags", "--", "-not-an-option");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(process).end(eq(-1), message.capture());
        assertThat(message.getValue()).contains("VM.flags", "-not-an-option");
    }

    @Test
    public void completeDiagnosticCommandName() {
        Completion completion = mock(Completion.class);
        when(completion.lineTokens()).thenReturn(Collections.<CliToken>singletonList(new CliTokenImpl(true, "VM.ver")));

        new JcmdCommand().complete(completion);

        verify(completion).complete("sion", true);
    }

    private static String executeSuccessfully(String... arguments) throws Throwable {
        CommandProcess process = mock(CommandProcess.class);
        execute(process, arguments);
        ArgumentCaptor<ResultModel> result = ArgumentCaptor.forClass(ResultModel.class);
        verify(process).appendResult(result.capture());
        verify(process).end();
        return ((MessageModel) result.getValue()).getMessage();
    }

    private static void execute(CommandProcess process, String... arguments) throws Throwable {
        JcmdCommand command = new JcmdCommand();
        CommandLine commandLine = CLIConfigurator.define(JcmdCommand.class, true).parse(Arrays.asList(arguments), true);
        CLIConfigurator.inject(commandLine, command);
        command.process(process);
    }
}
