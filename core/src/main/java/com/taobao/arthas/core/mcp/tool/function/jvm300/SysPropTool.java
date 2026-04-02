package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * SysProp 系统属性诊断工具类
 *
 * <p>该类提供了查看或修改 JVM 系统属性的功能。系统属性是 JVM 级别的配置信息，
 * 通过 {@code System.getProperties()} 和 {@code System.setProperty()} 进行访问。</p>
 *
 * <p>通过该工具可以：</p>
 * <ul>
 *   <li>查看所有系统属性</li>
 *   <li>查看指定系统属性的值</li>
 *   <li>修改或新增系统属性</li>
 * </ul>
 *
 * <p>系统属性通常包含了 JVM 的配置信息，如 Java 版本、类路径、操作系统信息等。
 * 修改系统属性可以动态调整 JVM 的运行时行为。</p>
 *
 * <p>对应 Arthas 命令行工具中的 {@code sysprop} 命令。</p>
 *
 * @author Arthas
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class SysPropTool extends AbstractArthasTool {

    /**
     * 查看或修改系统属性
     *
     * <p>该方法用于查看或修改 JVM 的系统属性。根据参数的不同组合，
     * 可以实现以下功能：</p>
     *
     * <ul>
     *   <li><strong>不提供任何参数：</strong>查看所有系统属性</li>
     *   <li><strong>只提供 propertyName：</strong>查看指定属性的值</li>
     *   <li><strong>提供 propertyName 和 propertyValue：</strong>修改或新增系统属性</li>
     * </ul>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>排查 JVM 配置问题</li>
     *   <li>检查特定系统属性是否正确设置</li>
     *   <li>动态调整 JVM 运行时参数</li>
     *   <li>临时修改系统配置进行测试</li>
     * </ul>
     *
     * <p><strong>注意：</strong>修改系统属性可能会影响应用程序的运行行为，
     * 请谨慎操作。某些系统属性可能无法被修改。</p>
     *
     * @param propertyName 系统属性名称。如果为 {@code null} 或空字符串，则查看所有属性；
     *                    如果指定了具体的属性名，则查看或修改该属性。
     *                    该参数会自动去除首尾空格。
     *                    该参数为可选参数。
     * @param propertyValue 系统属性值。如果指定了该值，则修改或新增指定的系统属性；
     *                     如果为 {@code null} 或空字符串，则只查看属性值。
     *                     该参数会自动去除首尾空格。
     *                     该参数为可选参数。
     * @param toolContext 工具执行上下文，包含了执行该工具所需的所有上下文信息，
     *                    如会话信息、权限信息等。
     * @return 执行结果的 JSON 字符串格式。
     *         <ul>
     *           <li>如果未提供属性名，返回所有系统属性的键值对列表</li>
     *           <li>如果只提供了属性名，返回该属性的当前值</li>
     *           <li>如果同时提供了属性名和属性值，返回修改操作的结果</li>
     *         </ul>
     */
    @Tool(
        name = "sysprop",
        description = "SysProp 诊断工具: 查看或修改系统属性，对应 Arthas 的 sysprop 命令。"
    )
    public String sysprop(
            @ToolParam(description = "属性名", required = false)
            String propertyName,

            @ToolParam(description = "属性值；若指定则修改，否则查看", required = false)
            String propertyValue,

            ToolContext toolContext
    ) {
        // 构建基础命令 "sysprop"
        StringBuilder cmd = buildCommand("sysprop");
        // 如果指定了属性名，并且不为空，则将其添加到命令中
        if (propertyName != null && !propertyName.trim().isEmpty()) {
            // 去除属性名首尾空格后添加到命令中
            cmd.append(" ").append(propertyName.trim());
            // 如果同时指定了属性值，并且不为空，则将其也添加到命令中
            if (propertyValue != null && !propertyValue.trim().isEmpty()) {
                // 去除属性值首尾空格后添加到命令中
                cmd.append(" ").append(propertyValue.trim());
            }
        }
        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}
