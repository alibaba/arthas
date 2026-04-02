package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * VMOption JVM虚拟机选项诊断工具类
 * 该工具用于查看或更新JVM的VM options（虚拟机选项）
 * 继承自AbstractArthasTool，提供vmoption命令的MCP工具接口
 *
 * @author Arthas
 * @see AbstractArthasTool
 */
public class VMOptionTool extends AbstractArthasTool {

    /**
     * 查看或更新JVM虚拟机选项
     * 该方法会构建vmoption命令并执行，用于查看或修改JVM的运行时参数
     * 当只提供key时，查看该选项的值；当同时提供key和value时，更新该选项的值
     *
     * @param key VM选项的名称，如"PrintGCDetails"，可为空（为空则列出所有选项）
     * @param value 要更新的值，仅在更新选项时使用，可为空
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return vmoption命令的执行结果，包含选项信息或更新结果
     */
    @Tool(
        name = "vmoption",
        description = "VMOption 诊断工具: 查看或更新 JVM VM options，对应 Arthas 的 vmoption 命令。"
    )
    public String vmoption(
            @ToolParam(description = "Name of the VM option.", required = false)
            String key,

            @ToolParam(description = "更新值，仅在更新时使用", required = false)
            String value,

            ToolContext toolContext
    ) {
        // 构建vmoption命令的基础部分
        StringBuilder cmd = buildCommand("vmoption");

        // 如果指定了选项名称（key），则添加到命令中
        if (key != null && !key.trim().isEmpty()) {
            // 添加选项名称，去除前后空格
            cmd.append(" ").append(key.trim());

            // 如果同时还指定了更新值（value），则添加到命令中
            // 这样会将该选项更新为指定值
            if (value != null && !value.trim().isEmpty()) {
                cmd.append(" ").append(value.trim());
            }
        }
        // 如果key为空，则不添加任何参数，命令将列出所有VM选项

        // 执行同步命令，返回操作结果
        return executeSync(toolContext, cmd.toString());
    }
}
