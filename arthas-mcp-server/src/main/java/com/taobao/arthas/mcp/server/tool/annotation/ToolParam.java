package com.taobao.arthas.mcp.server.tool.annotation;

import java.lang.annotation.*;

@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

	/**
	 * Whether the tool argument is required.
	 */
	boolean required() default true;

	/**
	 * The description of the tool argument.
	 */
	String description() default "";

}
