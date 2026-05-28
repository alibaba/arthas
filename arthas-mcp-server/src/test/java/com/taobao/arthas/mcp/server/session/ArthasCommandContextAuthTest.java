package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.task.DefaultCreateTaskContext;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ArthasCommandContextAuthTest {

    @Test
    void setSessionAuthShouldDelegateToCommandExecutorForBoundSession() {
        RecordingCommandExecutor commandExecutor = new RecordingCommandExecutor();
        ArthasCommandSessionManager.CommandSessionBinding binding =
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "arthas-session", "consumer");
        ArthasCommandContext commandContext = new ArthasCommandContext(commandExecutor, binding);
        Object authSubject = new Object();

        commandContext.setSessionAuth(authSubject);

        assertThat(commandExecutor.setSessionAuthCount).isEqualTo(1);
        assertThat(commandExecutor.lastAuthSessionId).isEqualTo("arthas-session");
        assertThat(commandExecutor.lastAuthSubject).isSameAs(authSubject);
    }

    @Test
    void setSessionAuthShouldIgnoreTemporaryContextAndNullSubject() {
        RecordingCommandExecutor commandExecutor = new RecordingCommandExecutor();
        ArthasCommandContext temporaryContext = new ArthasCommandContext(commandExecutor);
        ArthasCommandSessionManager.CommandSessionBinding binding =
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "arthas-session", "consumer");
        ArthasCommandContext boundContext = new ArthasCommandContext(commandExecutor, binding);

        temporaryContext.setSessionAuth(new Object());
        boundContext.setSessionAuth(null);

        assertThat(commandExecutor.setSessionAuthCount).isZero();
    }

    @Test
    void createIsolatedTaskSessionShouldInheritTransportAuthSubject() {
        RecordingCommandExecutor commandExecutor = new RecordingCommandExecutor();
        ArthasCommandSessionManager sessionManager = new ArthasCommandSessionManager(commandExecutor);
        ArthasCommandContext parentContext = new ArthasCommandContext(commandExecutor,
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "parent-session", "parent-consumer"));
        Object authSubject = new Object();
        DefaultMcpTransportContext transportContext = new DefaultMcpTransportContext();
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);
        McpNettyServerExchange exchange = new McpNettyServerExchange(
                "mcp-session", null, null, null, transportContext, null);
        DefaultCreateTaskContext createTaskContext = new DefaultCreateTaskContext(
                null, null, exchange, "mcp-session", 60000L, null, parentContext, sessionManager);

        ArthasCommandContext isolatedContext = createTaskContext.createIsolatedTaskSession("task-1");

        assertThat(isolatedContext.getSessionId()).isEqualTo("session-1");
        assertThat(commandExecutor.setSessionAuthCount).isEqualTo(1);
        assertThat(commandExecutor.lastAuthSessionId).isEqualTo("session-1");
        assertThat(commandExecutor.lastAuthSubject).isSameAs(authSubject);
    }

    private static final class RecordingCommandExecutor implements CommandExecutor {
        private final AtomicInteger nextSessionId = new AtomicInteger(1);
        private int setSessionAuthCount;
        private String lastAuthSessionId;
        private Object lastAuthSubject;

        @Override
        public Map<String, Object> executeSync(String commandLine, long timeout, String sessionId,
                                               Object authSubject, String userId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> executeAsync(String commandLine, String sessionId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> pullResults(String sessionId, String consumerId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> interruptJob(String sessionId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> createSession() {
            int sessionId = nextSessionId.getAndIncrement();
            Map<String, Object> result = new TreeMap<String, Object>();
            result.put("sessionId", "session-" + sessionId);
            result.put("consumerId", "consumer-" + sessionId);
            return result;
        }

        @Override
        public Map<String, Object> closeSession(String sessionId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public void setSessionAuth(String sessionId, Object authSubject) {
            this.setSessionAuthCount++;
            this.lastAuthSessionId = sessionId;
            this.lastAuthSubject = authSubject;
        }

        @Override
        public void setSessionUserId(String sessionId, String userId) {
        }
    }
}
