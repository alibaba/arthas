package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

import java.nio.file.Paths;

/**
 * 内存编译器工具类
 *
 * 该工具用于在内存中编译Java源代码文件，生成对应的class文件。
 * 主要应用场景包括：
 * - 在不重启应用的情况下编译Java代码
 * - 与 redefine 命令配合实现热更新
 * - 快速测试代码修改效果
 * - 在生产环境临时修复Bug
 * - 动态生成和加载类
 *
 * 工作流程：
 * 1. 使用 mc 命令编译.java文件生成.class文件
 * 2. 使用 redefine 命令将编译好的.class加载到JVM中
 * 3. JVM中的类即被替换为新代码
 *
 * 注意事项：
 * - 编译后的类需要与原类保持兼容（方法签名、字段等）
 * - 不支持添加/删除方法或字段
 * - 修改的类会在下次重启后恢复
 * - 建议在测试环境验证后再用于生产环境
 *
 * @author Arthas Team
 * @see AbstractArthasTool
 */
public class MemoryCompilerTool extends AbstractArthasTool {

    /**
     * 默认输出目录常量
     *
     * 默认情况下，编译后的.class文件会输出到工作目录下的 arthas-output 文件夹
     * 该目录会被转换为绝对路径，确保在不同工作目录下都能正确访问
     */
    public static final String DEFAULT_DUMP_DIR = Paths.get("arthas-output").toAbsolutePath().toString();

    /**
     * 内存编译器（Memory Compiler）的主方法
     *
     * 该方法在内存中编译Java源代码文件，无需使用外部javac命令。
     * 编译过程在JVM内部完成，支持指定类加载器以确保类路径正确。
     *
     * 典型使用场景：
     * 1. 线上Bug紧急修复：
     *    a. 编写修复后的Java代码保存为文件
     *    b. 使用 mc 命令编译生成.class文件
     *    c. 使用 redefine 命令将.class加载到JVM
     *    d. 验证修复效果
     *
     * 2. 代码调试和验证：
     *    - 快速验证代码修改思路
     *    - 测试不同的实现方案
     *    - 不需要重启应用
     *
     * 3. 动态功能扩展：
     *    - 运行时添加新的工具类
     *    - 动态生成辅助类
     *
     * 编译限制：
     * - 代码必须能通过语法检查
     * - 依赖的类必须在类路径中
     * - 不支持编译注解处理器
     * - 建议编译单个文件或少量文件
     *
     * @param javaFilePaths 要编译的Java源文件路径，支持：
     *                      - 单个文件：/tmp/Test.java
     *                      - 多个文件：/tmp/A.java /tmp/B.java（用空格分隔）
     *                      - 相对路径和绝对路径都支持
     *                      - 此参数为必填项
     * @param classLoaderHash 类加载器的hashcode（16进制格式）
     *                        - 指定使用哪个类加载器来编译
     *                        - 确保编译时能找到正确的依赖类
     *                        - 格式如：1a2b3c4d
     *                        - 通常建议指定，以避免类路径问题
     * @param classLoaderClass 类加载器的完整类名
     *                         - 如sun.misc.Launcher$AppClassLoader
     *                         - 可作为hashcode的替代方式
     *                         - 更直观但可能不够精确（可能有多个实例）
     * @param outputDir 指定编译后的.class文件输出目录
     *                  - 如果不指定，使用默认的 arthas-output 目录
     *                  - 建议使用绝对路径
     *                  - 目录不存在时会自动创建
     *                  - 后续的 redefine 命令需要使用此目录下的.class文件
     * @param toolContext 工具上下文对象，包含执行环境和配置信息
     * @return 编译结果的字符串表示，包含：
     *         - 编译成功：输出.class文件的保存路径
     *         - 编译失败：显示编译错误信息
     */
    @Tool(
            name = "mc",
            description = "Memory Compiler/内存编译器，编译.java文件生成.class"
    )
    public String mc(
            @ToolParam(description = "要编译的.java文件路径，支持多个文件，用空格分隔")
            String javaFilePaths,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "指定输出目录，默认为工作目录下arthas-output文件夹", required = false)
            String outputDir,

            ToolContext toolContext) {
        // 构建基础命令 "mc"（Memory Compiler的缩写）
        StringBuilder cmd = buildCommand("mc");

        // 添加要编译的Java文件路径（必填参数）
        // 支持多个文件，用空格分隔
        addParameter(cmd, javaFilePaths);

        // 处理类加载器标识参数
        // 优先使用hashcode，如果未提供则使用类名
        // 指定类加载器很重要，确保编译时能找到正确的类路径
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            // 使用16进制的hashcode指定类加载器
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            // 使用完整类名指定类加载器
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 处理输出目录参数
        // 如果指定了输出目录，使用指定的目录；否则使用默认目录
        if (outputDir != null && !outputDir.trim().isEmpty()) {
            // 使用用户指定的输出目录
            addParameter(cmd, "-d", outputDir);
        } else {
            // 使用默认的输出目录：arthas-output
            cmd.append(" -d ").append(DEFAULT_DUMP_DIR);
        }

        // 同步执行编译命令并返回结果
        // 返回内容包含编译状态和输出文件路径信息
        return executeSync(toolContext, cmd.toString());
    }
}
