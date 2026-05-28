package com.taobao.arthas.core.mcp.tool.function;

import com.taobao.arthas.core.mcp.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.ToolContextKeys;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractArthasToolAuthTest {

    @Test
    public void executeStreamableShouldApplyAuthBeforeAsyncExecution() {
        RecordingCommandExecutor commandExecutor = new RecordingCommandExecutor();
        ArthasCommandSessionManager.CommandSessionBinding binding =
                new ArthasCommandSessionManager.CommandSessionBinding("mcp-session", "arthas-session", "consumer");
        ArthasCommandContext commandContext = new ArthasCommandContext(commandExecutor, binding);
        Object authSubject = new Object();
        DefaultMcpTransportContext transportContext = new DefaultMcpTransportContext();
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);

        Map<String, Object> contextMap = new HashMap<String, Object>();
        contextMap.put(ToolContextKeys.COMMAND_CONTEXT, commandContext);
        contextMap.put(ToolContextKeys.MCP_TRANSPORT_CONTEXT, transportContext);

        new TestArthasTool().run(new ToolContext(contextMap));

        assertThat(commandExecutor.events).containsExactly("setSessionAuth", "executeAsync", "pullResults", "interruptJob");
        assertThat(commandExecutor.lastAuthSessionId).isEqualTo("arthas-session");
        assertThat(commandExecutor.lastAuthSubject).isSameAs(authSubject);
    }

    private static final class TestArthasTool extends AbstractArthasTool {
        private String run(ToolContext toolContext) {
            return executeStreamable(toolContext, "watch test", 0, 1, 100, "done");
        }
    }

    private static final class RecordingCommandExecutor implements CommandExecutor {
        private final List<String> events = new ArrayList<String>();
        private String lastAuthSessionId;
        private Object lastAuthSubject;

        @Override
        public Map<String, Object> executeSync(String commandLine, long timeout, String sessionId,
                                               Object authSubject, String userId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> executeAsync(String commandLine, String sessionId) {
            events.add("executeAsync");
            Map<String, Object> result = new TreeMap<String, Object>();
            result.put("success", true);
            result.put("command", commandLine);
            result.put("sessionId", sessionId);
            return result;
        }

        @Override
        public Map<String, Object> pullResults(String sessionId, String consumerId) {
            events.add("pullResults");
            Map<String, Object> result = new TreeMap<String, Object>();
            result.put("results", Collections.emptyList());
            result.put("jobStatus", "TERMINATED");
            return result;
        }

        @Override
        public Map<String, Object> interruptJob(String sessionId) {
            events.add("interruptJob");
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> createSession() {
            return new TreeMap<String, Object>();
        }

        @Override
        public Map<String, Object> closeSession(String sessionId) {
            return new TreeMap<String, Object>();
        }

        @Override
        public void setSessionAuth(String sessionId, Object authSubject) {
            events.add("setSessionAuth");
            this.lastAuthSessionId = sessionId;
            this.lastAuthSubject = authSubject;
        }

        @Override
        public void setSessionUserId(String sessionId, String userId) {
        }
    }
}
