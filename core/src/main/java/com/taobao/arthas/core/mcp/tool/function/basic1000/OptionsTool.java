package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Options MCP Tool: 查看和修改 Arthas 全局开关选项
 */
public class OptionsTool extends AbstractArthasTool {

    @Tool(
        name = "options",
        description = "Options 诊断工具: 查看或修改 Arthas 全局开关选项，对应 Arthas 的 options 命令。\n" +
                "使用示例:\n" +
                "- 不带参数: 列出所有选项\n" +
                "- 只指定 name: 查看指定选项的当前值\n" +
                "- 指定 name 和 value: 修改选项的值\n" +
                "常用选项:\n" +
                "- unsafe: 是否支持系统类增强（默认 false）\n" +
                "- dump: 是否 dump 增强后的类（默认 false）\n" +
                "- json-format: 是否使用 JSON 格式输出（默认 false）\n" +
                "- strict: 是否启用严格模式，禁止设置对象属性（默认 true）"
    )
    public String options(
            @ToolParam(description = "选项名称，如: unsafe, dump, json-format, strict 等", required = false)
            String name,

            @ToolParam(description = "选项值，用于修改选项时指定新值", required = false)
            String value,

            ToolContext toolContext
    ) {
        StringBuilder cmd = buildCommand("options");
        if (name != null && !name.trim().isEmpty()) {
            cmd.append(" ").append(name.trim());
            if (value != null && !value.trim().isEmpty()) {
                cmd.append(" ").append(value.trim());
            }
        }
        return executeSync(toolContext, cmd.toString());
    }
}
