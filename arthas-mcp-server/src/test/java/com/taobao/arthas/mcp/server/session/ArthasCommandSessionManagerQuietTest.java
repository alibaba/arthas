package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArthasCommandSessionManagerQuietTest {

    @Test
    void commandSessionsShouldBeCreatedQuietly() {
        RecordingCommandExecutor executor = new RecordingCommandExecutor();
        ArthasCommandSessionManager sessionManager = new ArthasCommandSessionManager(executor);

        sessionManager.createCommandSession("mcp-session");
        sessionManager.createIsolatedTaskSession("task-1");

        assertThat(executor.quietFlags).containsExactly(Boolean.TRUE, Boolean.TRUE);
    }

    private static final class RecordingCommandExecutor implements CommandExecutor {
        private final List<Boolean> quietFlags = new ArrayList<Boolean>();

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
        public Map<String, Object> createSession(boolean quiet) {
            quietFlags.add(quiet);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("sessionId", "session-" + quietFlags.size());
            result.put("consumerId", "consumer-" + quietFlags.size());
            return result;
        }

        @Override
        public Map<String, Object> closeSession(String sessionId) {
            return new HashMap<String, Object>();
        }

        @Override
        public void setSessionAuth(String sessionId, Object authSubject) {
        }

        @Override
        public void setSessionUserId(String sessionId, String userId) {
        }
    }
}
