package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.ToolContextKeys;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VMToolToolTest {

    @Test
    public void referenceAnalyzeShouldPassClassNameToCommand() {
        ArthasCommandContext commandContext = mock(ArthasCommandContext.class);
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(ToolContextKeys.COMMAND_CONTEXT, commandContext);

        new VMToolTool().vmtool(
                "referenceAnalyze", null, null, "java.lang.String",
                Integer.valueOf(5), null, null, null, new ToolContext(context));

        verify(commandContext).executeSync(
                "vmtool --action referenceAnalyze --className java.lang.String", null, null);
    }

    @Test
    public void getInstancesShouldKeepItsActionSpecificParameters() {
        ArthasCommandContext commandContext = mock(ArthasCommandContext.class);
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(ToolContextKeys.COMMAND_CONTEXT, commandContext);

        new VMToolTool().vmtool(
                "getInstances", "1a2b", null, "java.lang.String",
                Integer.valueOf(5), Integer.valueOf(2), "instances.length", null, new ToolContext(context));

        verify(commandContext).executeSync(
                "vmtool --action getInstances -c 1a2b --className java.lang.String"
                        + " --limit 5 -x 2 --express instances.length",
                null, null);
    }
}
