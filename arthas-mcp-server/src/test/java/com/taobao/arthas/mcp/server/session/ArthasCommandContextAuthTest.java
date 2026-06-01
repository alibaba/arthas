package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArthasCommandContextAuthTest {

    @Test
    void setSessionAuthShouldDelegateToBoundSession() {
        RecordingCommandExecutor executor = new RecordingCommandExecutor();
        ArthasCommandSessionManager.CommandSessionBinding binding =
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "arthas-session", "consumer");
        ArthasCommandContext context = new ArthasCommandContext(executor, binding);
        Object authSubject = new Object();

        context.setSessionAuth(authSubject);

        assertThat(executor.authSessionId).isEqualTo("arthas-session");
        assertThat(executor.authSubject).isSameAs(authSubject);
        assertThat(executor.authCallCount).isEqualTo(1);
    }

    @Test
    void setSessionAuthShouldIgnoreMissingBindingOrSubject() {
        RecordingCommandExecutor executor = new RecordingCommandExecutor();
        ArthasCommandContext temporaryContext = new ArthasCommandContext(executor);
        ArthasCommandSessionManager.CommandSessionBinding binding =
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "arthas-session", "consumer");
        ArthasCommandContext boundContext = new ArthasCommandContext(executor, binding);

        temporaryContext.setSessionAuth(new Object());
        boundContext.setSessionAuth(null);

        assertThat(executor.authCallCount).isZero();
    }

    private static final class RecordingCommandExecutor implements CommandExecutor {
        private String authSessionId;
        private Object authSubject;
        private int authCallCount;

        @Override
        public Map<String, Object> executeSync(String commandLine, long timeout, String sessionId,
                                               Object authSubject, String userId) {
            return new HashMap<String, Object>();
        }

        @Override
        public Map<String, Object> executeAsync(String commandLine, String sessionId) {
            return new HashMap<String, Object>();
        }

        @Override
        public Map<String, Object> pullResults(String sessionId, String consumerId) {
            return new HashMap<String, Object>();
        }

        @Override
        public Map<String, Object> interruptJob(String sessionId) {
            return new HashMap<String, Object>();
        }

        @Override
        public Map<String, Object> createSession() {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("sessionId", "created-session");
            result.put("consumerId", "created-consumer");
            return result;
        }

        @Override
        public Map<String, Object> closeSession(String sessionId) {
            return new HashMap<String, Object>();
        }

        @Override
        public void setSessionAuth(String sessionId, Object authSubject) {
            this.authSessionId = sessionId;
            this.authSubject = authSubject;
            this.authCallCount++;
        }

        @Override
        public void setSessionUserId(String sessionId, String userId) {
        }
    }
}
