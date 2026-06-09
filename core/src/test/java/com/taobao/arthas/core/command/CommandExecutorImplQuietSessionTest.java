package com.taobao.arthas.core.command;

import com.taobao.arthas.core.command.model.InputStatusModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.WelcomeModel;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.session.impl.SessionImpl;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommandExecutorImplQuietSessionTest {
    private final RecordingSessionManager sessionManager = new RecordingSessionManager();
    private final CommandExecutorImpl commandExecutor = new CommandExecutorImpl(sessionManager);

    @AfterEach
    void tearDown() {
        sessionManager.close();
    }

    @Test
    void createSessionShouldAppendWelcomeByDefault() {
        Map<String, Object> result = commandExecutor.createSession(false);
        List<ResultModel> results = pollResults((String) result.get("consumerId"));

        assertThat(results).anyMatch(MessageModel.class::isInstance);
        assertThat(results).anyMatch(WelcomeModel.class::isInstance);
        assertThat(results).anyMatch(InputStatusModel.class::isInstance);
        assertThat((Object) sessionManager.lastSession.get(Session.QUIET)).isNull();
    }

    @Test
    void createQuietSessionShouldSkipWelcomeModelsAndKeepInputStatus() {
        Map<String, Object> result = commandExecutor.createSession(true);
        List<ResultModel> results = pollResults((String) result.get("consumerId"));

        assertThat(results).noneMatch(MessageModel.class::isInstance);
        assertThat(results).noneMatch(WelcomeModel.class::isInstance);
        assertThat(results).anyMatch(InputStatusModel.class::isInstance);
        assertThat((Object) sessionManager.lastSession.get(Session.QUIET)).isEqualTo(Boolean.TRUE);
    }

    private List<ResultModel> pollResults(String consumerId) {
        SharingResultDistributor distributor = sessionManager.lastSession.getResultDistributor();
        ResultConsumer consumer = distributor.getConsumer(consumerId);
        return consumer.pollResults();
    }

    private static final class RecordingSessionManager implements SessionManager {
        private final InternalCommandManager commandManager =
                new InternalCommandManager(Collections.<CommandResolver>emptyList());
        private final JobControllerImpl jobController = new JobControllerImpl();
        private Session lastSession;

        @Override
        public Session createSession() {
            Session session = new SessionImpl();
            session.put(Session.COMMAND_MANAGER, commandManager);
            session.put(Session.PID, 123L);
            session.put(Session.ID, UUID.randomUUID().toString());
            this.lastSession = session;
            return session;
        }

        @Override
        public Session getSession(String sessionId) {
            if (lastSession != null && lastSession.getSessionId().equals(sessionId)) {
                return lastSession;
            }
            return null;
        }

        @Override
        public Session removeSession(String sessionId) {
            Session session = getSession(sessionId);
            if (session != null && session.getResultDistributor() != null) {
                session.getResultDistributor().close();
            }
            lastSession = null;
            return session;
        }

        @Override
        public void updateAccessTime(Session session) {
            session.setLastAccessTime(System.currentTimeMillis());
        }

        @Override
        public void close() {
            if (lastSession != null && lastSession.getResultDistributor() != null) {
                lastSession.getResultDistributor().close();
            }
        }

        @Override
        public InternalCommandManager getCommandManager() {
            return commandManager;
        }

        @Override
        public Instrumentation getInstrumentation() {
            return null;
        }

        @Override
        public JobController getJobController() {
            return jobController;
        }
    }
}
