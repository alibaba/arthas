package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.ToolContextKeys;
import org.junit.Assert;
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

    @Test
    public void referenceAnalyzeShouldRejectMissingOrBlankClassName() {
        assertClassNameRequired("referenceAnalyze", null);
        assertClassNameRequired("referenceAnalyze", "  ");
    }

    @Test
    public void getInstancesShouldRejectMissingOrBlankClassName() {
        assertClassNameRequired("getInstances", null);
        assertClassNameRequired("getInstances", "  ");
    }

    private static void assertClassNameRequired(String action, String className) {
        ArthasCommandContext commandContext = mock(ArthasCommandContext.class);
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(ToolContextKeys.COMMAND_CONTEXT, commandContext);

        try {
            new VMToolTool().vmtool(
                    action, null, null, className,
                    null, null, null, null, new ToolContext(context));
            Assert.fail("Expected className validation to fail");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("vmtool " + action + " 需要指定类名 (className)", e.getMessage());
        }
    }
}
