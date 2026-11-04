package com.taobao.arthas.core.mcp.tool.function;

import com.taobao.arthas.core.mcp.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.ToolContextKeys;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractArthasToolAuthTest {

    @Test
    public void executeStreamableShouldApplyAuthSubjectBeforeAsyncExecution() {
        ArthasCommandContext commandContext = mock(ArthasCommandContext.class);
        String command = "trace demo.MathGame run";
        Map<String, Object> asyncResult = new HashMap<String, Object>();
        asyncResult.put("success", false);
        asyncResult.put("error", "start failed");
        when(commandContext.executeAsync(command)).thenReturn(asyncResult);

        DefaultMcpTransportContext transportContext = new DefaultMcpTransportContext();
        Object authSubject = new Object();
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);
        transportContext.put(McpAuthExtractor.MCP_USER_ID_KEY, "user-1");

        Map<String, Object> context = new HashMap<String, Object>();
        context.put(ToolContextKeys.COMMAND_CONTEXT, commandContext);
        context.put(ToolContextKeys.MCP_TRANSPORT_CONTEXT, transportContext);

        new TestArthasTool().callExecuteStreamable(new ToolContext(context), command);

        InOrder inOrder = inOrder(commandContext);
        inOrder.verify(commandContext).setSessionAuth(authSubject);
        inOrder.verify(commandContext).setSessionUserId("user-1");
        inOrder.verify(commandContext).executeAsync(command);
    }

    private static final class TestArthasTool extends AbstractArthasTool {
        private String callExecuteStreamable(ToolContext toolContext, String command) {
            return executeStreamable(toolContext, command, 1, 10, 1000, null);
        }
    }
}
