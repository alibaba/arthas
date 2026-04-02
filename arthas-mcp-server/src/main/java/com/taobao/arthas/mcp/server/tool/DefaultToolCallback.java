package com.taobao.arthas.mcp.server.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;
import com.taobao.arthas.mcp.server.tool.execution.ToolCallResultConverter;
import com.taobao.arthas.mcp.server.tool.execution.ToolExecutionException;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.JsonParser;
import com.taobao.arthas.mcp.server.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 默认工具回调实现类
 * <p>
 * 负责将工具调用请求转换为实际的方法调用。
 * 该类通过反射机制执行被@Tool注解标记的方法，并处理参数转换、结果转换等逻辑。
 * </p>
 */
public class DefaultToolCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallback.class);

    /**
     * 工具定义对象
     * <p>
     * 包含工具的元数据信息，如名称、描述、参数定义等
     * </p>
     */
    private final ToolDefinition toolDefinition;

    /**
     * 工具方法对象
     * <p>
     * 实际要执行的方法，通过反射获取
     * </p>
     */
    private final Method toolMethod;

    /**
     * 工具对象实例
     * <p>
     * 如果是实例方法，需要持有对象实例；如果是静态方法，则为null
     * </p>
     */
    private final Object toolObject;

    /**
     * 工具调用结果转换器
     * <p>
     * 负责将方法执行结果转换为统一的字符串格式
     * </p>
     */
    private final ToolCallResultConverter toolCallResultConverter;


    /**
     * 构造函数
     *
     * @param toolDefinition 工具定义对象
     * @param toolMethod 工具方法对象
     * @param toolObject 工具对象实例（静态方法可为null）
     * @param toolCallResultConverter 结果转换器
     * @throws IllegalArgumentException 如果参数验证失败
     */
    public DefaultToolCallback(ToolDefinition toolDefinition, Method toolMethod,
                              Object toolObject, ToolCallResultConverter toolCallResultConverter) {
        // 参数非空校验
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        Assert.notNull(toolMethod, "toolMethod cannot be null");
        // 非静态方法必须有对象实例
        Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
                "toolObject cannot be null for non-static methods");
        this.toolDefinition = toolDefinition;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallResultConverter = toolCallResultConverter;
    }

    /**
     * 获取工具定义对象
     *
     * @return 工具定义对象
     */
    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    /**
     * 调用工具（无上下文）
     *
     * @param toolInput JSON格式的工具输入参数
     * @return 执行结果的JSON字符串
     */
    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    /**
     * 调用工具（带上下文）
     * <p>
     * 执行工具方法的完整流程：
     * 1. 验证输入参数
     * 2. 验证工具上下文支持
     * 3. 提取工具参数
     * 4. 验证必填参数
     * 5. 构建方法参数数组
     * 6. 反射调用方法
     * 7. 转换返回结果
     * </p>
     *
     * @param toolInput JSON格式的工具输入参数
     * @param toolContext 工具上下文对象
     * @return 执行结果的JSON字符串
     */
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        // 校验工具输入参数非空
        Assert.hasText(toolInput, "toolInput cannot be null or empty");

        logger.debug("Starting execution of tool: {}", this.toolDefinition.getName());

        // 验证工具上下文支持
        validateToolContextSupport(toolContext);

        // 从JSON中提取工具参数
        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        // 验证必填参数
        validateRequiredParameters(toolArguments);

        // 构建方法调用参数数组
        Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

        // 反射调用方法
        Object result = callMethod(methodArguments);

        logger.debug("Successful execution of tool: {}", this.toolDefinition.getName());

        // 获取方法的返回类型
        Type returnType = this.toolMethod.getGenericReturnType();

        // 将执行结果转换为JSON字符串
        return this.toolCallResultConverter.convert(result, returnType);
    }

    /**
     * 验证工具上下文支持
     * <p>
     * 检查方法是否需要ToolContext参数，如果需要但没有提供，则抛出异常
     * </p>
     *
     * @param toolContext 工具上下文对象
     * @throws IllegalArgumentException 如果方法需要ToolContext但未提供
     */
    private void validateToolContextSupport(ToolContext toolContext) {
        // 检查是否提供了非空的工具上下文
        boolean isNonEmptyToolContextProvided = toolContext != null && !Utils.isEmpty(toolContext.getContext());

        // 检查方法参数中是否包含ToolContext类型
        boolean isToolContextAcceptedByMethod = Arrays.stream(this.toolMethod.getParameterTypes())
                .anyMatch(type -> Utils.isAssignable(type, ToolContext.class));

        // 如果方法需要ToolContext但未提供，抛出异常
        if (isToolContextAcceptedByMethod && !isNonEmptyToolContextProvided) {
            throw new IllegalArgumentException("ToolContext is required by the method as an argument");
        }
    }

    /**
     * 验证必填参数
     * <p>
     * 检查所有标记为required的参数是否都有值
     * </p>
     *
     * @param toolArguments 工具参数Map
     * @throws IllegalArgumentException 如果必填参数缺失或为空
     */
    private void validateRequiredParameters(Map<String, Object> toolArguments) {
        Parameter[] parameters = this.toolMethod.getParameters();

        // 遍历方法的所有参数
        for (Parameter parameter : parameters) {
            // 跳过ToolContext类型的参数
            if (parameter.getType().isAssignableFrom(ToolContext.class)) {
                continue;
            }

            // 获取参数的ToolParam注解
            ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
            // 检查是否为必填参数
            if (toolParam != null && toolParam.required()) {
                String paramName = parameter.getName();
                Object paramValue = toolArguments.get(paramName);

                // 检查参数是否为null
                if (paramValue == null) {
                    throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
                }

                // 检查字符串参数是否为空字符串
                if (paramValue instanceof String && ((String) paramValue).trim().isEmpty()) {
                    throw new IllegalArgumentException("Required parameter '" + paramName + "' cannot be empty");
                }
            }
        }
    }

    /**
     * 从JSON字符串中提取工具参数
     *
     * @param toolInput JSON格式的工具输入参数
     * @return 参数键值对Map
     */
    private Map<String, Object> extractToolArguments(String toolInput) {
        // 使用JSON解析器将JSON字符串解析为Map对象
        return JsonParser.fromJson(toolInput, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 构建方法调用参数数组
     * <p>
     * 将工具参数Map转换为方法调用所需的对象数组
     * </p>
     *
     * @param toolInputArguments 工具参数Map
     * @param toolContext 工具上下文对象
     * @return 方法参数对象数组
     */
    private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, ToolContext toolContext) {
        // 使用Stream处理方法参数列表
        return Stream.of(this.toolMethod.getParameters()).map(parameter -> {
            // 如果参数类型是ToolContext，直接返回上下文对象
            if (parameter.getType().isAssignableFrom(ToolContext.class)) {
                return toolContext;
            }
            // 从参数Map中获取原始参数值
            Object rawArgument = toolInputArguments.get(parameter.getName());
            // 将原始参数值转换为方法参数所需的具体类型
            return buildTypedArgument(rawArgument, parameter.getParameterizedType());
        }).toArray();
    }

    /**
     * 构建带类型的参数对象
     * <p>
     * 将原始参数值转换为目标类型的对象
     * </p>
     *
     * @param value 原始参数值
     * @param type 目标类型
     * @return 转换后的参数对象
     */
    private Object buildTypedArgument(Object value, Type type) {
        // 如果值为null，直接返回null
        if (value == null) {
            return null;
        }

        // 如果目标类型是普通Class对象，直接转换
        if (type instanceof Class<?>) {
            return JsonParser.toTypedObject(value, (Class<?>) type);
        }

        // 处理泛型类型：先转JSON再解析为目标类型
        String json = JsonParser.toJson(value);
        return JsonParser.fromJson(json, type);
    }

    /**
     * 反射调用方法
     * <p>
     * 使用反射机制执行工具方法，并处理各种异常情况
     * </p>
     *
     * @param methodArguments 方法参数数组
     * @return 方法执行结果
     * @throws IllegalStateException 如果方法访问失败
     * @throws ToolExecutionException 如果方法执行异常
     */
    private Object callMethod(Object[] methodArguments) {
        // 如果对象类或方法不是public的，设置可访问权限
        if (isObjectNotPublic() || isMethodNotPublic()) {
            this.toolMethod.setAccessible(true);
        }
        Object result;
        try {
            // 反射调用方法
            result = this.toolMethod.invoke(this.toolObject, methodArguments);
        }
        catch (IllegalAccessException ex) {
            // 访问权限异常
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        }
        catch (InvocationTargetException ex) {
            // 方法执行异常，包装为工具执行异常
            throw new ToolExecutionException(this.toolDefinition, ex.getCause());
        }
        return result;
    }

    /**
     * 检查对象是否不是public
     *
     * @return 如果对象不是public则返回true
     */
    private boolean isObjectNotPublic() {
        return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
    }

    /**
     * 检查方法是否不是public
     *
     * @return 如果方法不是public则返回true
     */
    private boolean isMethodNotPublic() {
        return !Modifier.isPublic(this.toolMethod.getModifiers());
    }

    /**
     * 生成对象的字符串表示
     *
     * @return 对象的字符串描述
     */
    @Override
    public String toString() {
        return "MethodToolCallback{" + "toolDefinition=" + this.toolDefinition + '}';
    }

    /**
     * 创建Builder实例
     *
     * @return Builder对象
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder构建器类
     * <p>
     * 使用Builder模式创建DefaultToolCallback实例
     * </p>
     */
    public static final class Builder {

        /**
         * 工具定义对象
         */
        private ToolDefinition toolDefinition;

        /**
         * 工具方法对象
         */
        private Method toolMethod;

        /**
         * 工具对象实例
         */
        private Object toolObject;

        /**
         * 结果转换器
         */
        private ToolCallResultConverter toolCallResultConverter;

        /**
         * 私有构造函数
         */
        private Builder() {
        }

        /**
         * 设置工具定义对象
         *
         * @param toolDefinition 工具定义对象
         * @return Builder实例
         */
        public Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }

        /**
         * 设置工具方法对象
         *
         * @param toolMethod 工具方法对象
         * @return Builder实例
         */
        public Builder toolMethod(Method toolMethod) {
            this.toolMethod = toolMethod;
            return this;
        }

        /**
         * 设置工具对象实例
         *
         * @param toolObject 工具对象实例
         * @return Builder实例
         */
        public Builder toolObject(Object toolObject) {
            this.toolObject = toolObject;
            return this;
        }

        /**
         * 设置结果转换器
         *
         * @param toolCallResultConverter 结果转换器
         * @return Builder实例
         */
        public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
            this.toolCallResultConverter = toolCallResultConverter;
            return this;
        }

        /**
         * 构建DefaultToolCallback实例
         *
         * @return DefaultToolCallback实例
         */
        public DefaultToolCallback build() {
            return new DefaultToolCallback(this.toolDefinition, this.toolMethod, this.toolObject, this.toolCallResultConverter);
        }

    }
}
