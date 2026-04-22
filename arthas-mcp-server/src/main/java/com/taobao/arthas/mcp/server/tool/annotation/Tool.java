package com.taobao.arthas.mcp.server.tool.annotation;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.TaskSupportMode;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    String name() default "";

    String description() default "";

    boolean streamable() default false;
    
    /**
     * 任务支持模式。
     *
     * <ul>
     *   <li>{@link TaskSupportMode#FORBIDDEN FORBIDDEN} - 不支持任务（默认）</li>
     *   <li>{@link TaskSupportMode#OPTIONAL OPTIONAL} - 可选支持任务</li>
     *   <li>{@link TaskSupportMode#REQUIRED REQUIRED} - 必须以任务模式调用</li>
     * </ul>
     *
     * @return 任务支持模式
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/tasks">MCP Tasks Specification</a>
     */
    TaskSupportMode taskSupport() default TaskSupportMode.FORBIDDEN;

}
