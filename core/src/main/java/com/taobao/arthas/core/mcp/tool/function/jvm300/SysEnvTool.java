package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * SysEnv 系统环境变量诊断工具类
 *
 * <p>该类提供了查看系统环境变量的功能。环境变量是操作系统级别的配置信息，
 * 包含了诸如路径、用户信息、系统配置等重要信息。</p>
 *
 * <p>通过该工具可以：</p>
 * <ul>
 *   <li>查看所有系统环境变量</li>
 *   <li>查看指定环境变量的值</li>
 * </ul>
 *
 * <p>对应 Arthas 命令行工具中的 {@code sysenv} 命令。</p>
 *
 * @author Arthas
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class SysEnvTool extends AbstractArthasTool {

    /**
     * 查看系统环境变量
     *
     * <p>该方法用于查看 JVM 进程的系统环境变量。可以根据参数选择查看所有环境变量
     * 或查看特定环境变量的值。</p>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>排查环境配置问题</li>
     *   <li>检查特定环境变量是否正确设置</li>
     *   <li>了解运行环境的配置信息</li>
     * </ul>
     *
     * @param envName 环境变量名称。如果为 {@code null} 或空字符串，则查看所有环境变量；
     *               如果指定了具体的变量名，则只查看该变量的值。
     *               该参数会自动去除首尾空格。
     *               该参数为可选参数。
     * @param toolContext 工具执行上下文，包含了执行该工具所需的所有上下文信息，
     *                    如会话信息、权限信息等。
     * @return 执行结果的 JSON 字符串格式。如果指定了环境变量名，返回该变量的值；
     *         如果未指定，返回所有环境变量的键值对列表。
     */
    @Tool(
        name = "sysenv",
        description = "SysEnv 诊断工具: 查看系统环境变量，对应 Arthas 的 sysenv 命令。"
    )
    public String sysenv(
            @ToolParam(description = "环境变量名。若为空或空字符串，则查看所有变量；否则查看单个变量值。", required = false)
            String envName,
            ToolContext toolContext
    ) {
        // 构建基础命令 "sysenv"
        StringBuilder cmd = buildCommand("sysenv");
        // 如果指定了环境变量名，并且不为空，则将其添加到命令中
        if (envName != null && !envName.trim().isEmpty()) {
            // 去除变量名首尾空格后添加到命令中
            cmd.append(" ").append(envName.trim());
        }
        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}
