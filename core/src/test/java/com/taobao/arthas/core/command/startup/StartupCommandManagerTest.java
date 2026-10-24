package com.taobao.arthas.core.command.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.session.impl.SessionImpl;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

public class StartupCommandManagerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadCommandsShouldIgnoreCommentsBlankLinesAndBom() throws Exception {
        Path script = temporaryFolder.newFile("startup.as").toPath();
        String content = "\uFEFF# comment\n\n  watch demo.MathGame run  \n"
                        + "  # another comment\nline --class demo.MathGame --line 42\n";
        Files.write(script, content.getBytes(StandardCharsets.UTF_8));

        assertThat(StartupCommandManager.loadCommands(script)).containsExactly(
                        "watch demo.MathGame run",
                        "line --class demo.MathGame --line 42");
    }

    @Test
    public void loadCommandsShouldRejectTooManyCommands() throws Exception {
        Path script = temporaryFolder.newFile("too-many.as").toPath();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i <= StartupCommandManager.MAX_COMMANDS; i++) {
            content.append("echo ").append(i).append('\n');
        }
        Files.write(script, content.toString().getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> StartupCommandManager.loadCommands(script))
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("more than " + StartupCommandManager.MAX_COMMANDS);
    }

    @Test
    public void loadCommandsShouldRejectLongCommand() throws Exception {
        Path script = temporaryFolder.newFile("too-long.as").toPath();
        StringBuilder command = new StringBuilder();
        for (int i = 0; i <= StartupCommandManager.MAX_COMMAND_LENGTH; i++) {
            command.append('a');
        }
        Files.write(script, command.toString().getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> StartupCommandManager.loadCommands(script))
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("exceeds " + StartupCommandManager.MAX_COMMAND_LENGTH);
    }

    @Test
    public void startShouldUseIndependentStartupSessionsForCommands() throws Exception {
        Path script = temporaryFolder.newFile("multiple.as").toPath();
        Files.write(script, ("echo first\necho second\n").getBytes(StandardCharsets.UTF_8));
        File outputRoot = temporaryFolder.newFolder("output");

        final List<Session> sessions = new ArrayList<Session>();
        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.createSession()).thenAnswer(invocation -> {
            Session session = new SessionImpl();
            session.put(Session.ID, UUID.randomUUID().toString());
            session.put(Session.PID, 123L);
            sessions.add(session);
            return session;
        });

        JobController jobController = mock(JobController.class);
        AtomicInteger jobIds = new AtomicInteger();
        List<Job> jobs = new ArrayList<Job>();
        when(jobController.createJob(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Job job = mock(Job.class);
            when(job.id()).thenReturn(jobIds.incrementAndGet());
            when(job.run(anyBoolean())).thenReturn(job);
            jobs.add(job);
            return job;
        });

        ExecutorService executor = mock(ExecutorService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        StartupCommandManager manager = new StartupCommandManager(sessionManager, jobController,
                        mock(InternalCommandManager.class), executor, outputRoot);
        manager.start(script.toFile());

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).getSessionId()).isNotEqualTo(sessions.get(1).getSessionId());
        assertThat((Object) sessions.get(0).get(Session.STARTUP_COMMAND)).isEqualTo(Boolean.TRUE);
        assertThat((Object) sessions.get(1).get(Session.STARTUP_COMMAND)).isEqualTo(Boolean.TRUE);
        assertThat((Object) sessions.get(0).get(Session.QUIET)).isEqualTo(Boolean.TRUE);
        assertThat(jobs).hasSize(2);
        verify(jobs.get(0)).run(true);
        verify(jobs.get(1)).run(true);
        verify(sessionManager, times(2)).createSession();

        JSONObject manifest = JSON.parseObject(new String(Files.readAllBytes(
                        new File(manager.getRunDirectory(), "manifest.json").toPath()), StandardCharsets.UTF_8));
        assertThat(manifest.getJSONArray("commands")).hasSize(2);
        assertThat(manifest.getJSONArray("commands").getJSONObject(0).getString("resultFile"))
                        .isEqualTo("command-001.jsonl");

        manager.close();
    }
}
