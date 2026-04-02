package com.taobao.arthas.mcp.server.tool.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法（或复合注解）为 MCP Tool（工具）。
 * <p>
 * MCP（Model Context Protocol）Tool 是 LLM 可以调用的能力单元。
 * 被此注解标记的方法会在启动时被扫描注册到 MCP Server，
 * 客户端（如 LLM Agent）可通过 MCP 协议发现并调用这些工具。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Tool(name = "exec", description = "执行 Arthas 命令并返回结果")
 * public String exec(@ToolParam(description = "命令行") String command) {
 *     // ...
 * }
 * }</pre>
 *
 * <p>作用目标：方法（{@link ElementType#METHOD}）或其他注解（{@link ElementType#ANNOTATION_TYPE}，用于组合注解场景）。
 * <p>生命周期：运行时保留（{@link RetentionPolicy#RUNTIME}），可通过反射读取。
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * Tool 的名称，用于在 MCP 协议中唯一标识该工具。
     * <p>
     * 若不指定（保持默认空字符串），框架通常会使用被标注方法的方法名作为 Tool 名称。
     * 建议显式指定，以保持接口稳定，不受方法重命名影响。
     *
     * @return Tool 名称，默认为空字符串
     */
    String name() default "";

    /**
     * Tool 的描述信息，向 LLM 解释该工具的用途和使用场景。
     * <p>
     * LLM 依赖此描述决策是否调用本工具以及如何构造参数，
     * 建议使用清晰、准确的自然语言描述工具的功能、适用场景及预期输出。
     *
     * @return Tool 描述，默认为空字符串
     */
    String description() default "";

    /**
     * 是否支持流式输出（Streamable Tool）。
     * <p>
     * 当设置为 {@code true} 时，表示该工具支持以流的方式逐步向客户端推送执行结果，
     * 适用于执行时间较长、可分批输出的命令（如实时监控类命令）。
     * 设置为 {@code false}（默认）时，工具执行完毕后一次性返回全部结果。
     *
     * @return 是否支持流式输出，默认为 {@code false}
     */
    boolean streamable() default false;

}
