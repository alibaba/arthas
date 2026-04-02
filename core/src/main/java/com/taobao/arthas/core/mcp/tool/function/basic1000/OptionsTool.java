package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Options 诊断工具类
 * 提供查看和修改 Arthas 全局开关选项的功能
 * 对应 Arthas 命令行工具的 options 命令
 *
 * 功能说明：
 * - 查看所有全局选项及其当前值
 * - 查看特定选项的当前值
 * - 修改全局选项的值
 * - 控制 Arthas 的各种行为开关
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class OptionsTool extends AbstractArthasTool {

    /**
     * options 诊断工具方法: 查看或修改 Arthas 全局开关选项
     *
     * 功能说明：
     * - 不带参数：列出所有全局选项及其当前值
     * - 只指定 name：查看指定选项的当前值和描述
     * - 指定 name 和 value：修改选项的值
     *
     * 常用选项说明：
     * - unsafe: 是否支持系统类增强（默认 false）
     *           开启后可以增强 JDK 内部类，但可能导致 JVM 不稳定
     *
     * - dump: 是否 dump 增强后的类到文件系统（默认 false）
     *         开启后会将增强的类保存到 ${user.home}/arthas-class-dump/ 目录
     *
     * - json-format: 是否使用 JSON 格式输出（默认 false）
     *               开启后命令输出将使用 JSON 格式，便于程序解析
     *
     * - strict: 是否启用严格模式（默认 true）
     *           开启后将禁止通过 OGNL 设置对象属性，提高安全性
     *
     * - save-result: 是否保存命令执行结果（默认 false）
     *               开启后可以在后续命令中通过 #result 引用上一次的结果
     *
     * - job-timeout: 异步任务超时时间（默认，单位：毫秒）
     *               控制 async-job 等异步命令的最大执行时间
     *
     * 使用场景：
     * - 需要增强系统类时开启 unsafe 选项
     * - 需要保存增强后的类文件时开启 dump 选项
     * - 需要程序化解析输出时开启 json-format 选项
     * - 需要在 OGNL 中设置对象属性时关闭 strict 选项
     *
     * @param name 选项名称，例如: unsafe, dump, json-format, strict, save-result, job-timeout 等
     *             如果为空或不提供，则列出所有选项
     * @param value 选项的新值，用于修改选项时指定
     *              只有在指定 name 时才有效
     *              值的类型取决于具体选项（通常是 true/false 或数字）
     * @param toolContext 工具执行上下文，包含会话信息等
     * @return 选项的当前值或修改结果字符串
     */
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
        // 构建基础命令
        StringBuilder cmd = buildCommand("options");

        // 检查是否指定了选项名称
        if (name != null && !name.trim().isEmpty()) {
            // 添加选项名称，去除首尾空格
            cmd.append(" ").append(name.trim());

            // 检查是否同时指定了选项值
            // 只有在指定了选项名称的情况下，选项值才有意义
            if (value != null && !value.trim().isEmpty()) {
                // 添加选项值，去除首尾空格
                cmd.append(" ").append(value.trim());
            }
        }

        // 同步执行命令并返回结果
        // 如果只指定 name：返回该选项的当前值和描述
        // 如果指定 name 和 value：返回修改结果
        // 如果都不指定：返回所有选项的列表
        return executeSync(toolContext, cmd.toString());
    }
}
