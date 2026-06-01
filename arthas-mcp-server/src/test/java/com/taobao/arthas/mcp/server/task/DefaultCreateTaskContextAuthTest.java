package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCreateTaskContextAuthTest {

    @Test
    void createIsolatedTaskSessionShouldApplyTransportAuthSubject() {
        RecordingCommandExecutor executor = new RecordingCommandExecutor();
        ArthasCommandContext commandContext = new ArthasCommandContext(executor,
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "main-session", "main-consumer"));
        ArthasCommandSessionManager sessionManager = new ArthasCommandSessionManager(executor);
        DefaultMcpTransportContext transportContext = new DefaultMcpTransportContext();
        Object authSubject = new Object();
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);
        McpNettyServerExchange exchange =
                new McpNettyServerExchange("mcp-session", null, null, null, transportContext, null);
        DefaultCreateTaskContext createTaskContext = new DefaultCreateTaskContext(
                null, null, exchange, "mcp-session", null, null, commandContext, sessionManager);

        ArthasCommandContext isolatedContext = createTaskContext.createIsolatedTaskSession("task-1");

        assertThat(isolatedContext.getSessionId()).isEqualTo("isolated-session");
        assertThat(executor.authSessionId).isEqualTo("isolated-session");
        assertThat(executor.authSubject).isSameAs(authSubject);
        assertThat(executor.authCallCount).isEqualTo(1);
    }

    @Test
    void createIsolatedTaskSessionShouldNotApplyAuthWhenTransportAuthMissing() {
        RecordingCommandExecutor executor = new RecordingCommandExecutor();
        ArthasCommandContext commandContext = new ArthasCommandContext(executor,
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "main-session", "main-consumer"));
        ArthasCommandSessionManager sessionManager = new ArthasCommandSessionManager(executor);
        McpNettyServerExchange exchange = new McpNettyServerExchange(
                "mcp-session", null, null, null, new DefaultMcpTransportContext(), null);
        DefaultCreateTaskContext createTaskContext = new DefaultCreateTaskContext(
                null, null, exchange, "mcp-session", null, null, commandContext, sessionManager);

        createTaskContext.createIsolatedTaskSession("task-1");

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
            result.put("sessionId", "isolated-session");
            result.put("consumerId", "isolated-consumer");
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
