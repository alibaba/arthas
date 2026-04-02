package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Heapdump 诊断工具类
 * 提供生成 JVM 堆转储文件的功能，对应 Arthas 的 heapdump 命令
 *
 * 功能特性：
 * - 生成 JVM 堆内存的快照文件（.hprof 格式）
 * - 支持只转储存活对象（通过 --live 选项）
 * - 支持自定义输出文件路径
 * - 自动创建输出目录（如果不存在）
 * - 自动生成带时间戳的文件名（当未指定输出路径时）
 */
public class HeapdumpTool extends AbstractArthasTool {

    /**
     * 默认的堆转储文件输出目录
     * 使用 "arthas-output" 目录的绝对路径
     * 将 Windows 风格的反斜杠替换为正斜杠以保证跨平台兼容性
     */
    public static final String DEFAULT_DUMP_DIR = Paths.get("arthas-output").toAbsolutePath().toString().replace("\\", "/");

    /**
     * 时间戳格式化器
     * 用于生成堆转储文件名中的时间戳部分
     * 格式：yyyyMMdd_HHmmss，例如：20240315_143022
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 生成 JVM 堆转储文件
     *
     * 功能说明：
     * 生成当前 JVM 的堆内存快照，可以用于分析内存泄漏、对象分布等问题
     *
     * 支持的参数:
     * - live: 是否只转储存活对象（对象是否被 GC Roots 引用）
     * - filePath: 输出文件路径，若为空则使用默认的 arthas-output 目录并自动生成文件名
     *
     * @param live 是否只 dump 存活对象（--live 选项）
     *             true - 只转储可达对象（被 GC Roots 引用的对象）
     *             false - 转储堆中的所有对象
     *             null - 不添加 --live 参数
     * @param filePath 指定输出文件的完整路径
     *                 如果未指定，则在 arthas-output 目录下生成带时间戳的文件
     *                 时间戳格式：heapdump_yyyyMMdd_HHmmss.hprof
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return 诊断结果的字符串形式，包含堆转储文件的保存路径和相关信息
     * @throws IOException 如果创建输出目录或文件失败
     */
    @Tool(
            name = "heapdump",
            description = "Heapdump 诊断工具: 生成 JVM heap dump，支持 --live 选项。对应 Arthas 的 heapdump 命令。"
    )
    public String heapdump(
            @ToolParam(description = "是否只 dump 存活对象 (--live)", required = false)
            Boolean live,

            @ToolParam(description = "指定输出文件路径，默认为当前工作目录下的arthas-output文件夹中的时间戳命名的.hprof文件", required = false)
            String filePath,

            ToolContext toolContext
    ) throws IOException {
        // 最终使用的文件路径
        String finalFilePath;

        // 确定最终的输出文件路径
        if (filePath != null && !filePath.trim().isEmpty()) {
            // 如果用户指定了文件路径，则使用用户提供的路径
            // 将 Windows 风格的反斜杠替换为正斜杠以保证跨平台兼容性
            finalFilePath = filePath.trim().replace("\\", "/");
        } else {
            // 如果用户未指定文件路径，则使用默认路径
            // 获取默认输出目录的 Path 对象
            Path defaultDir = Paths.get(DEFAULT_DUMP_DIR);

            // 检查默认目录是否存在，如果不存在则创建
            if (!Files.exists(defaultDir)) {
                Files.createDirectories(defaultDir);
            }

            // 生成当前时间的时间戳字符串
            // 格式：yyyyMMdd_HHmmss，例如：20240315_143022
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

            // 生成默认文件名：heapdump_[时间戳].hprof
            String defaultFileName = String.format("heapdump_%s.hprof", timestamp);

            // 组合完整的文件路径
            finalFilePath = Paths.get(DEFAULT_DUMP_DIR, defaultFileName).toString().replace("\\", "/");
        }

        // 构建 heapdump 命令的基础部分
        StringBuilder cmd = buildCommand("heapdump");

        // 添加 --live 选项（如果需要）
        // addFlag 方法会在 live 为 true 时添加 "--live" 标志
        addFlag(cmd, "--live", live);

        // 添加输出文件路径参数
        cmd.append(" ").append(finalFilePath);

        // 同步执行命令并返回结果
        // 返回结果包含堆转储文件的保存路径和执行状态信息
        return executeSync(toolContext, cmd.toString());
    }
}
