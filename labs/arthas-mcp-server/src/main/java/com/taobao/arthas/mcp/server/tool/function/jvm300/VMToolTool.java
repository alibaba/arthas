package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.MCP_TRANSPORT_CONTEXT;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class VMToolTool {

    public static final String ACTION_GET_INSTANCES = "getInstances";
    public static final String ACTION_FORCE_GC = "forceGc";
    public static final String ACTION_INTERRUPT_THREAD = "interruptThread";

    @Tool(
        name = "vmtool",
        description = "虚拟机工具诊断工具: 查询实例、强制 GC、线程中断等，对应 Arthas 的 vmtool 命令。"
    )
    public String vmtool(
            @ToolParam(description = "操作类型: getInstances/forceGc/interruptThread 等")
            String action,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "类名，全限定（getInstances 时使用）", required = false)
            String className,

            @ToolParam(description = "返回实例限制数量 (-l)，getInstances 时使用，默认 10；<=0 表示不限制", required = false)
            Integer limit,

            @ToolParam(description = "结果对象展开层次 (-x)，默认 1", required = false)
            Integer expandLevel,

            @ToolParam(description = "OGNL 表达式，对 getInstances 返回的 instances 执行 (--express)", required = false)
            String express,

            @ToolParam(description = "线程 ID (-t)，interruptThread 时使用", required = false)
            Long threadId,

            ToolContext toolContext
    ) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        McpTransportContext mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
        Object authSubject = mcpTransportContext.get(MCP_AUTH_SUBJECT_KEY);
        StringBuilder cmd = new StringBuilder("vmtool");
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("vmtool: action 参数不能为空");
        }
        cmd.append(" --action ").append(action.trim());

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHash.trim());
        }
        if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
        }
        if (ACTION_GET_INSTANCES.equals(action.trim())) {
            if (className != null && !className.trim().isEmpty()) {
                cmd.append(" --className ").append(className.trim());
            }
            if (limit != null) {
                cmd.append(" --limit ").append(limit);
            }
            if (expandLevel != null && expandLevel > 0) {
                cmd.append(" -x ").append(expandLevel);
            }
            if (express != null && !express.trim().isEmpty()) {
                cmd.append(" --express ").append(express.trim());
            }
        }

        // interruptThread
        if (ACTION_INTERRUPT_THREAD.equals(action.trim())) {
            if (threadId != null && threadId > 0) {
                cmd.append(" -t ").append(threadId);
            } else {
                throw new IllegalArgumentException("vmtool interruptThread 需要指定线程 ID (threadId)");
            }
        }

        String commandStr = cmd.toString();
        return JsonParser.toJson(commandContext.executeSync(commandStr, authSubject));
    }


}
