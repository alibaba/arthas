package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.model.JadModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JadCommandTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void decompileClassFileWithoutLoadingIt() throws Throwable {
        File classFile = copyClassFile();
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "--file", classFile.getAbsolutePath());

        ArgumentCaptor<ResultModel> results = ArgumentCaptor.forClass(ResultModel.class);
        verify(process, times(2)).appendResult(results.capture());
        List<ResultModel> models = results.getAllValues();
        JadModel result = (JadModel) models.get(0);
        assertThat(result.getSource()).contains("multiply(", "* 7", "jad-file");
        assertThat(result.getLocation()).isEqualTo(classFile.getAbsolutePath());
        assertThat(result.getClassInfo()).isNull();
        assertThat(models.get(1)).isInstanceOf(RowAffectModel.class);
        verify(process, never()).session();
        verify(process).end();
    }

    @Test
    public void decompileMethodFromClassFileWithSourceOptions() throws Throwable {
        File classFile = copyClassFile();
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "--file", "--source-only", "--lineNumber=false", classFile.getAbsolutePath(), "multiply");

        ArgumentCaptor<ResultModel> results = ArgumentCaptor.forClass(ResultModel.class);
        verify(process).appendResult(results.capture());
        JadModel result = (JadModel) results.getValue();
        assertThat(result.getSource()).contains("multiply(", "* 7").doesNotContain("jad-file", "/*");
        assertThat(result.getLocation()).isNull();
        assertThat(result.getMappings()).isNotEmpty();
        verify(process, never()).session();
        verify(process).end();
    }

    @Test
    public void rejectMissingClassFile() throws Throwable {
        File classFile = new File(temporaryFolder.getRoot(), "missing.class");
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "--file", classFile.getAbsolutePath());

        verify(process).end(eq(-1), anyString());
        verify(process, never()).session();
    }

    @Test
    public void rejectNonClassFile() throws Throwable {
        File classFile = temporaryFolder.newFile("invalid.class");
        Files.write(classFile.toPath(), "not bytecode".getBytes(StandardCharsets.UTF_8));
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "--file", classFile.getAbsolutePath());

        verify(process).end(eq(-1), anyString());
        verify(process, never()).session();
    }

    @Test
    public void rejectClassLoaderOptionsForClassFiles() throws Throwable {
        File classFile = copyClassFile();
        CommandProcess process = mock(CommandProcess.class);

        execute(process, "--file", "-c", "1234", classFile.getAbsolutePath());

        verify(process).end(eq(-1), anyString());
        verify(process, never()).session();
    }

    @Test
    public void completeClassFilePath() throws Exception {
        File classFile = temporaryFolder.newFile("Target.class");
        Completion completion = mock(Completion.class);
        when(completion.lineTokens()).thenReturn(Arrays.asList(
                new CliTokenImpl(true, "--file"), new CliTokenImpl(false, " "),
                new CliTokenImpl(true, classFile.getParent() + "/Tar")));

        new JadCommand().complete(completion);

        verify(completion).complete("get.class", true);
        verify(completion, never()).session();
    }

    @Test
    public void preserveClassNameSearch() throws Throwable {
        CommandProcess process = mock(CommandProcess.class);
        Session session = mock(Session.class);
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(process.session()).thenReturn(session);
        when(session.getInstrumentation()).thenReturn(instrumentation);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[0]);

        execute(process, "sample/Target");

        verify(instrumentation).getAllLoadedClasses();
        verify(process).end(-1, "No class found for: sample.Target");
    }

    private File copyClassFile() throws Exception {
        File classFile = temporaryFolder.newFile("decompile target.class");
        try (InputStream input = getClass().getResourceAsStream("JadCommandTest$DecompileTarget.class")) {
            Files.copy(input, classFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return classFile;
    }

    private static void execute(CommandProcess process, String... args) throws Throwable {
        JadCommand command = new JadCommand();
        CommandLine commandLine = CLIConfigurator.define(JadCommand.class, true).parse(Arrays.asList(args), true);
        CLIConfigurator.inject(commandLine, command);
        command.process(process);
    }

    public static class DecompileTarget {
        public int multiply(int value) {
            return value * 7;
        }

        public String message() {
            return "jad-file";
        }
    }
}
