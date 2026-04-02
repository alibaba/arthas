package com.taobao.arthas.mcp.server.tool.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单的 JSON Schema 生成工具类。
 * <p>
 * 专用于根据 Java 方法的参数列表生成符合 JSON Schema 规范的描述对象（{@link McpSchema.JsonSchema}），
 * 供 MCP Server 向客户端（LLM Agent）暴露 Tool 的输入参数规格。
 * <p>
 * 支持的参数注解：
 * <ul>
 *   <li>{@link ToolParam}：自定义参数描述和必填性</li>
 *   <li>{@link JsonProperty}：自定义参数名称和必填性</li>
 *   <li>{@link JsonPropertyDescription}：自定义参数描述</li>
 * </ul>
 * <p>
 * 支持的 Java 类型到 JSON Schema 类型映射：
 * <ul>
 *   <li>{@code String} → {@code "string"}</li>
 *   <li>{@code int/Integer/long/Long} → {@code "integer"}</li>
 *   <li>{@code double/Double/float/Float} → {@code "number"}</li>
 *   <li>{@code boolean/Boolean} → {@code "boolean"}</li>
 *   <li>数组类型 → {@code "array"}，并递归推断元素类型</li>
 *   <li>其他对象类型 → {@code "object"}</li>
 * </ul>
 * <p>
 * 本类为工具类，不可实例化，所有方法均为静态方法。
 */
public final class JsonSchemaGenerator {

    /** 共享的 Jackson ObjectMapper 实例，用于构造 JSON 节点，线程安全 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 参数必填性的默认值。
     * <p>
     * 当参数上既没有 {@link ToolParam} 也没有 {@link JsonProperty} 注解时，
     * 以此值作为参数是否必填的默认判断结果（{@code true} 表示默认必填）。
     */
    private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;

    /**
     * 私有构造方法，禁止外部实例化。
     * 本类为纯静态工具类，无需也不应被实例化。
     */
    private JsonSchemaGenerator() {
    }

    /**
     * 为指定方法的输入参数生成 JSON Schema 对象。
     * <p>
     * 遍历方法的所有参数，对其中标注了 {@link ToolParam} 注解的参数：
     * <ol>
     *   <li>推断参数名称（优先使用 {@link JsonProperty#value()}，其次使用反射获取的参数名）</li>
     *   <li>推断参数类型对应的 JSON Schema 类型（调用 {@link #generateParameterProperties}）</li>
     *   <li>判断参数是否必填（调用 {@link #isParameterRequired}）</li>
     *   <li>获取参数描述（调用 {@link #getParameterDescription}）</li>
     * </ol>
     * 最终生成一个顶层类型为 {@code "object"} 的 JSON Schema，
     * {@code properties} 字段描述各参数规格，{@code required} 字段列出必填参数名，
     * 并设置 {@code additionalProperties: false} 禁止额外参数。
     *
     * @param method 目标方法，不能为 {@code null}
     * @return 描述该方法输入参数规格的 {@link McpSchema.JsonSchema} 对象
     * @throws IllegalArgumentException 若 {@code method} 为 {@code null}
     */
    public static McpSchema.JsonSchema generateForMethodInput(Method method) {
        Assert.notNull(method, "method cannot be null");

        // 创建顶层 JSON Schema 节点，类型固定为 "object"
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");

        // properties 节点：存储各参数的 JSON Schema 定义
        ObjectNode properties = schema.putObject("properties");
        // required 列表：收集所有必填参数的名称
        List<String> required = new ArrayList<>();

        // 遍历方法的所有参数，仅处理标注了 @ToolParam 的参数
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
            if (toolParam == null) {
                // 未标注 @ToolParam 的参数跳过，不纳入 JSON Schema
                continue;
            }

            // 推断参数名称：优先取 @JsonProperty 中的显式名称，否则使用反射获得的参数名
            String paramName = getParameterName(parameter);
            Class<?> paramType = parameter.getType();

            // 判断参数是否必填，必填的参数名加入 required 列表
            boolean isRequired = isParameterRequired(parameter);
            if (isRequired) {
                required.add(paramName);
            }

            // 根据参数的 Java 类型生成对应的 JSON Schema 类型描述节点
            ObjectNode paramProperties = generateParameterProperties(paramType);

            // 获取参数描述，若存在则写入 JSON Schema 的 description 字段
            String description = getParameterDescription(parameter);
            if (description != null) {
                paramProperties.put("description", description);
            }

            // 将该参数的 Schema 定义注册到 properties 节点中
            properties.set(paramName, paramProperties);
        }

        // 若有必填参数，将 required 数组写入顶层 Schema 节点
        if (!required.isEmpty()) {
            ArrayNode requiredArray = schema.putArray("required");
            for (String req : required) {
                requiredArray.add(req);
            }
        }

        // 禁止额外属性，严格校验客户端传入的参数字段
        schema.put("additionalProperties", false);

        // 将 ObjectNode 形式的 properties 转换为普通 Map，构造并返回 JsonSchema 对象
        return new McpSchema.JsonSchema("object", convertToMap(properties), required, false);
    }

    /**
     * 获取参数在 JSON Schema 中使用的名称。
     * <p>
     * 解析优先级：
     * <ol>
     *   <li>若参数上标注了 {@link JsonProperty} 且 {@code value()} 非空，则使用该值</li>
     *   <li>否则使用反射获得的参数名（依赖编译器保留参数名，需使用 {@code -parameters} 编译选项）</li>
     * </ol>
     *
     * @param parameter 方法参数对象
     * @return 参数在 JSON Schema 中的字段名
     */
    private static String getParameterName(Parameter parameter) {
        JsonProperty jsonProperty = parameter.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
            // @JsonProperty 中明确指定了参数名，优先使用
            return jsonProperty.value();
        }
        // 回退到反射获取的参数名
        return parameter.getName();
    }

    /**
     * 根据 Java 参数类型生成对应的 JSON Schema 类型描述节点。
     * <p>
     * 类型映射规则如下：
     * <ul>
     *   <li>{@code String} → {@code {"type": "string"}}</li>
     *   <li>{@code int/Integer/long/Long} → {@code {"type": "integer"}}</li>
     *   <li>{@code double/Double/float/Float} → {@code {"type": "number"}}</li>
     *   <li>{@code boolean/Boolean} → {@code {"type": "boolean"}}</li>
     *   <li>数组类型 → {@code {"type": "array", "items": {...}}}，
     *       其中 {@code items} 根据数组元素类型递归推断（仅支持一层）</li>
     *   <li>其他所有引用类型 → {@code {"type": "object"}}</li>
     * </ul>
     *
     * @param paramType 参数的 Java 类型
     * @return 描述该类型的 JSON Schema {@link ObjectNode}
     */
    private static ObjectNode generateParameterProperties(Class<?> paramType) {
        ObjectNode properties = OBJECT_MAPPER.createObjectNode();

        if (paramType == String.class) {
            // 字符串类型
            properties.put("type", "string");
        } else if (paramType == int.class || paramType == Integer.class
                || paramType == long.class || paramType == Long.class) {
            // 整数类型（包括 int/Integer 和 long/Long）
            properties.put("type", "integer");
        } else if (paramType == double.class || paramType == Double.class
                || paramType == float.class || paramType == Float.class) {
            // 浮点数类型（包括 double/Double 和 float/Float）
            properties.put("type", "number");
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            // 布尔类型
            properties.put("type", "boolean");
        } else if (paramType.isArray()) {
            // 数组类型：顶层标记为 "array"，并通过 items 描述元素类型
            properties.put("type", "array");
            ObjectNode items = properties.putObject("items");
            Class<?> componentType = paramType.getComponentType();

            // 推断数组元素的 JSON Schema 类型
            if (componentType == String.class) {
                items.put("type", "string");
            } else if (componentType == int.class || componentType == Integer.class
                    || componentType == long.class || componentType == Long.class) {
                items.put("type", "integer");
            } else if (componentType == double.class || componentType == Double.class
                    || componentType == float.class || componentType == Float.class) {
                items.put("type", "number");
            } else if (componentType == boolean.class || componentType == Boolean.class) {
                items.put("type", "boolean");
            } else {
                // 其他引用类型数组元素，统一标记为 "object"
                items.put("type", "object");
            }
        } else {
            // 其他所有未匹配的类型（包括复杂 POJO），统一标记为 "object"
            properties.put("type", "object");
        }

        return properties;
    }

    /**
     * 判断方法参数在 JSON Schema 中是否为必填项。
     * <p>
     * 解析优先级：
     * <ol>
     *   <li>若参数上标注了 {@link ToolParam}，则以 {@link ToolParam#required()} 的值为准</li>
     *   <li>否则若标注了 {@link JsonProperty}，则以 {@link JsonProperty#required()} 的值为准</li>
     *   <li>若两者均未标注，则以 {@link #PROPERTY_REQUIRED_BY_DEFAULT}（{@code true}）作为默认值</li>
     * </ol>
     *
     * @param parameter 方法参数对象
     * @return 若参数为必填项则返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isParameterRequired(Parameter parameter) {
        ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
        if (toolParam != null) {
            // @ToolParam 优先级最高，直接使用其 required 属性
            return toolParam.required();
        }

        JsonProperty jsonProperty = parameter.getAnnotation(JsonProperty.class);
        if (jsonProperty != null) {
            // 其次使用 @JsonProperty 的 required 属性
            return jsonProperty.required();
        }

        // 两者均未标注时，回退到默认值（true，即默认必填）
        return PROPERTY_REQUIRED_BY_DEFAULT;
    }

    /**
     * 获取方法参数的描述信息，用于填充 JSON Schema 的 {@code description} 字段。
     * <p>
     * 解析优先级：
     * <ol>
     *   <li>若参数上标注了 {@link ToolParam} 且 {@link ToolParam#description()} 非空，
     *       则使用该描述</li>
     *   <li>否则若标注了 {@link JsonPropertyDescription} 且其值非空，则使用该描述</li>
     *   <li>若两者均无描述，则返回 {@code null}（不写入 description 字段）</li>
     * </ol>
     *
     * @param parameter 方法参数对象
     * @return 参数描述字符串，若无描述则返回 {@code null}
     */
    private static String getParameterDescription(Parameter parameter) {
        ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
        if (toolParam != null && toolParam.description() != null && !toolParam.description().isEmpty()) {
            // @ToolParam 中有非空描述，优先使用
            return toolParam.description();
        }

        JsonPropertyDescription jsonPropertyDescription = parameter.getAnnotation(JsonPropertyDescription.class);
        if (jsonPropertyDescription != null && !jsonPropertyDescription.value().isEmpty()) {
            // 其次使用 @JsonPropertyDescription 中的描述
            return jsonPropertyDescription.value();
        }

        // 无描述信息，返回 null
        return null;
    }

    /**
     * 将 Jackson {@link ObjectNode} 递归转换为普通的 {@link Map} 结构。
     * <p>
     * 转换规则：
     * <ul>
     *   <li>ObjectNode 对应的每个字段：
     *     <ul>
     *       <li>若字段值为 ObjectNode，则递归调用本方法转换为嵌套 Map</li>
     *       <li>若字段值为 ArrayNode，则转换为 {@link List}，列表元素同样递归处理</li>
     *       <li>其他基本类型节点（字符串、数字、布尔）通过 {@code asText()} 转为字符串</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * 该方法主要用于将 JSON Schema 的 {@code properties} 节点转换为
     * {@link McpSchema.JsonSchema} 构造方法所需的 {@code Map<String, Object>} 格式。
     *
     * @param node 待转换的 Jackson ObjectNode
     * @return 转换后的普通 Map，键为字段名，值为对应的 Java 对象（Map、List 或 String）
     */
    private static Map<String, Object> convertToMap(ObjectNode node) {
        Map<String, Object> result = new HashMap<>();
        // 遍历 ObjectNode 的所有字段
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isObject()) {
                // 嵌套对象：递归转换为 Map
                result.put(entry.getKey(), convertToMap((ObjectNode) value));
            } else if (value.isArray()) {
                // 数组：遍历元素，对象元素递归转换，基本类型元素转为字符串
                List<Object> array = new ArrayList<>();
                value.elements().forEachRemaining(element -> {
                    if (element.isObject()) {
                        array.add(convertToMap((ObjectNode) element));
                    } else {
                        array.add(element.asText());
                    }
                });
                result.put(entry.getKey(), array);
            } else {
                // 基本类型（字符串、数字、布尔等），统一转为字符串存储
                result.put(entry.getKey(), value.asText());
            }
        });
        return result;
    }
}
