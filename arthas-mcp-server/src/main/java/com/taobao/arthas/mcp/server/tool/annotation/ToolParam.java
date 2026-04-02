package com.taobao.arthas.mcp.server.tool.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法参数、字段或复合注解为 MCP Tool 的输入参数。
 * <p>
 * 配合 {@link Tool} 注解使用，用于描述 MCP Tool 方法中各参数的元信息，
 * 包括参数是否必填以及参数的自然语言描述。
 * 框架在构建 MCP Tool 的 JSON Schema 时，会读取此注解的信息填充参数规格，
 * 以便 LLM 正确理解并构造调用参数。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Tool(name = "exec", description = "执行 Arthas 命令")
 * public String exec(
 *     @ToolParam(description = "要执行的 Arthas 命令行，例如：watch com.example.Service method")
 *     String command,
 *
 *     @ToolParam(required = false, description = "命令执行超时时间，单位毫秒，默认 30000")
 *     Long timeout
 * ) {
 *     // ...
 * }
 * }</pre>
 *
 * <p>作用目标：方法参数（{@link ElementType#PARAMETER}）、字段（{@link ElementType#FIELD}）
 * 或其他注解（{@link ElementType#ANNOTATION_TYPE}，用于组合注解场景）。
 * <p>生命周期：运行时保留（{@link RetentionPolicy#RUNTIME}），可通过反射读取。
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

	/**
	 * 参数是否为必填项。
	 * <p>
	 * 若设置为 {@code true}（默认），则该参数会出现在 JSON Schema 的 {@code required} 列表中，
	 * LLM 在调用工具时必须提供该参数的值。
	 * 若设置为 {@code false}，则该参数为可选项，LLM 可以不传入该参数。
	 *
	 * @return 是否必填，默认为 {@code true}
	 */
	boolean required() default true;

	/**
	 * 参数的描述信息，向 LLM 解释该参数的含义、取值范围和使用方式。
	 * <p>
	 * 该描述会写入工具的 JSON Schema 中对应参数的 {@code description} 字段，
	 * LLM 依赖此信息理解参数语义并生成合适的参数值。
	 * 建议描述尽量具体，包括：参数的用途、合法取值示例、默认值说明等。
	 *
	 * @return 参数描述，默认为空字符串
	 */
	String description() default "";

}
