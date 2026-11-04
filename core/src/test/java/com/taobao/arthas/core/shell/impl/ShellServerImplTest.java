package com.taobao.arthas.core.shell.impl;

import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.server.SessionClosedHandler;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ShellServerImplTest {

    @Test
    void closeShouldWaitForAllTermServersWhenThereAreNoSessions() {
        ShellServerImpl server = newServer();
        RecordingTermServer first = new RecordingTermServer();
        RecordingTermServer second = new RecordingTermServer();
        server.registerTermServer(first);
        server.registerTermServer(second);

        AtomicInteger completionCount = new AtomicInteger();
        server.close(result -> completionCount.incrementAndGet());

        assertThat(completionCount.get()).isEqualTo(0);
        first.completeClose();
        assertThat(completionCount.get()).isEqualTo(0);
        second.completeClose();
        assertThat(completionCount.get()).isEqualTo(1);
    }

    @Test
    void closeShouldWaitForSessionAndAllTermServers() throws Exception {
        ShellServerImpl server = newServer();
        RecordingTermServer first = new RecordingTermServer();
        RecordingTermServer second = new RecordingTermServer();
        server.registerTermServer(first);
        server.registerTermServer(second);
        TestShell shell = addSession(server);

        AtomicInteger completionCount = new AtomicInteger();
        server.close(result -> completionCount.incrementAndGet());

        first.completeClose();
        second.completeClose();
        assertThat(completionCount.get()).isEqualTo(0);
        shell.closedFuture.complete();
        assertThat(completionCount.get()).isEqualTo(1);
    }

    @Test
    void closeShouldWaitForAllSessionsAndTermServers() throws Exception {
        ShellServerImpl server = newServer();
        RecordingTermServer termServer = new RecordingTermServer();
        server.registerTermServer(termServer);
        TestShell first = addSession(server);
        TestShell second = addSession(server);

        AtomicInteger completionCount = new AtomicInteger();
        server.close(result -> completionCount.incrementAndGet());

        termServer.completeClose();
        first.closedFuture.complete();
        assertThat(completionCount.get()).isEqualTo(0);
        second.closedFuture.complete();
        assertThat(completionCount.get()).isEqualTo(1);
    }

    private static ShellServerImpl newServer() {
        ShellServerImpl server = new ShellServerImpl(new ShellServerOptions());
        server.setClosed(false);
        return server;
    }

    @SuppressWarnings("unchecked")
    private static TestShell addSession(ShellServerImpl server) throws Exception {
        TestShell shell = new TestShell();
        Field sessionsField = ShellServerImpl.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, ShellImpl> sessions = (Map<String, ShellImpl>) sessionsField.get(server);
        sessions.put(shell.id, shell);
        shell.closedFuture.setHandler(new SessionClosedHandler(server, shell));
        return shell;
    }

    private static class TestShell extends ShellImpl {
        TestShell() {
            super(null, null, new InternalCommandManager(Collections.emptyList()), null, 0L, new JobControllerImpl());
        }

        @Override
        public void close(String reason) {
        }
    }

    private static class RecordingTermServer extends TermServer {
        private Handler<Future<Void>> closeHandler;

        @Override
        public TermServer termHandler(Handler<Term> handler) {
            return this;
        }

        @Override
        public TermServer listen(Handler<Future<TermServer>> listenHandler) {
            return this;
        }

        @Override
        public int actualPort() {
            return 0;
        }

        @Override
        public void close() {
        }

        @Override
        public void close(Handler<Future<Void>> completionHandler) {
            closeHandler = completionHandler;
        }

        void completeClose() {
            closeHandler.handle(Future.<Void>succeededFuture());
        }
    }
}
