package com.taobao.arthas.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 嵌套配置注解
 * <p>
 * 该注解用于标记需要进行嵌套配置处理的字段。当一个配置类中包含另一个配置类作为属性时，
 * 可以使用此注解标记该字段，以便在配置解析时进行特殊处理。
 * </p>
 * <p>
 * 该注解在运行时保留，可以通过反射机制读取，主要用于配置管理框架中识别嵌套的配置对象。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * public class GlobalOptions {
 *     &#64;NestedConfig
 *     private AgentInfo agentInfo;
 * }
 * </pre>
 * </p>
 *
 * @author hengyunabc 2019-08-05
 *
 */
@Retention(RetentionPolicy.RUNTIME)  // 注解在运行时保留，可以通过反射读取
@Target(ElementType.FIELD)           // 该注解只能用于字段上
public @interface NestedConfig {

}