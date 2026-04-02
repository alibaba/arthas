package com.taobao.arthas.mcp.server.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * JSON 与 Java 对象互相转换的工具类。
 * <p>
 * 整合了两个 JSON 库：
 * <ul>
 *   <li><b>FastJSON2</b>（{@link JSON}）：序列化（{@link #toJson}）时优先使用，
 *       支持注册自定义 {@link ValueFilter} 对序列化结果进行过滤或脱敏。</li>
 *   <li><b>Jackson</b>（{@link ObjectMapper}）：作为 FastJSON2 的降级方案，
 *       同时也是反序列化（{@link #fromJson}）的主要实现之一。</li>
 * </ul>
 * <p>
 * Jackson ObjectMapper 的配置：
 * <ul>
 *   <li>反序列化时忽略 JSON 中存在但 Java 类中不存在的字段（{@code FAIL_ON_UNKNOWN_PROPERTIES = false}）</li>
 *   <li>序列化时不因 Bean 无属性而失败（{@code FAIL_ON_EMPTY_BEANS = false}）</li>
 *   <li>注册 {@link JavaTimeModule}，支持 Java 8 日期时间类型（如 {@code LocalDateTime}）</li>
 *   <li>日期时间不以时间戳格式输出，而是以 ISO-8601 字符串格式输出（{@code WRITE_DATES_AS_TIMESTAMPS = false}）</li>
 * </ul>
 * <p>
 * 本类为工具类，不可实例化，所有方法均为静态方法。
 */
public final class JsonParser {

	private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);

	/**
	 * 共享的 Jackson ObjectMapper 实例，通过 {@link #createObjectMapper()} 初始化，线程安全。
	 * 用于 JSON 合法性校验、反序列化，以及 FastJSON2 序列化失败时的降级序列化。
	 */
	private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

	/**
	 * 全局注册的 FastJSON2 {@link ValueFilter} 列表。
	 * <p>
	 * 使用 {@link CopyOnWriteArrayList} 保证多线程并发注册/清除过滤器时的线程安全。
	 * 过滤器在 {@link #toJson(Object)} 序列化时按注册顺序依次应用，
	 * 可用于字段脱敏（如屏蔽密码、Token 等敏感字段）或自定义值转换。
	 */
	private static final List<ValueFilter> JSON_FILTERS = new CopyOnWriteArrayList<>();

	/**
	 * 注册一个自定义的 FastJSON2 序列化值过滤器。
	 * <p>
	 * 过滤器在每次调用 {@link #toJson(Object)} 时生效，可用于：
	 * <ul>
	 *   <li>对敏感字段（密码、Token 等）进行脱敏替换</li>
	 *   <li>过滤或转换特定字段的序列化值</li>
	 * </ul>
	 * 若 {@code filter} 为 {@code null}，则忽略本次注册（不抛异常）。
	 *
	 * @param filter 待注册的 FastJSON2 {@link ValueFilter}，为 {@code null} 时跳过
	 */
	public static void registerFilter(ValueFilter filter) {
		if (filter != null) {
			JSON_FILTERS.add(filter);
		}
	}

	/**
	 * 清除所有已注册的 FastJSON2 序列化值过滤器。
	 * <p>
	 * 调用后，{@link #toJson(Object)} 将不再应用任何自定义过滤器，恢复为默认序列化行为。
	 * 通常用于测试环境重置状态，或在需要关闭脱敏的场景中使用。
	 */
	public static void clearFilters() {
		JSON_FILTERS.clear();
	}

	/**
	 * 创建并配置 Jackson {@link ObjectMapper} 实例。
	 * <p>
	 * 配置项说明：
	 * <ul>
	 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false}：反序列化时忽略 JSON 中多余的字段，
	 *       增强接口的向后兼容性。</li>
	 *   <li>{@code FAIL_ON_EMPTY_BEANS = false}：允许序列化没有任何可序列化属性的 Java Bean，
	 *       避免在特殊类（如代理类）上抛出异常。</li>
	 *   <li>{@link JavaTimeModule}：支持 Java 8 {@code java.time.*} 日期时间类型的序列化/反序列化。</li>
	 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false}：日期时间以 ISO-8601 字符串格式输出
	 *       （如 {@code "2024-01-01T12:00:00"}），而非毫秒时间戳，可读性更好。</li>
	 * </ul>
	 *
	 * @return 配置完成的 Jackson {@link ObjectMapper} 实例
	 */
	private static ObjectMapper createObjectMapper() {
		return JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.addModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();
	}

	/**
	 * 私有构造方法，禁止外部实例化。
	 */
	private JsonParser() {
	}

	/**
	 * 获取内部共享的 Jackson {@link ObjectMapper} 实例。
	 * <p>
	 * 返回的实例为全局单例，线程安全，可直接用于额外的 JSON 操作，
	 * 例如读取 JSON 树（{@code readTree}）或构造 Java 类型（{@code constructType}）。
	 *
	 * @return 全局共享的 {@link ObjectMapper} 实例
	 */
	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	/**
	 * 将 JSON 字符串反序列化为指定的 Java 类型（Class 方式）。
	 * <p>
	 * 优先使用 FastJSON2 解析；若 FastJSON2 抛出异常，则回退到 Jackson 解析。
	 * 若两者均失败，则抛出 {@link IllegalStateException}，并以 FastJSON2 的异常作为根因。
	 *
	 * @param <T>  目标类型
	 * @param json 待解析的 JSON 字符串，不能为 {@code null}
	 * @param type 目标 Java 类型，不能为 {@code null}
	 * @return 反序列化得到的 Java 对象
	 * @throws IllegalArgumentException 若 {@code json} 或 {@code type} 为 {@code null}
	 * @throws IllegalStateException    若 FastJSON2 和 Jackson 均解析失败
	 */
	public static <T> T fromJson(String json, Class<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			// 优先使用 FastJSON2 解析
			return JSON.parseObject(json, type);
		}
		catch (Exception ex) {
			// FastJSON2 失败，降级到 Jackson
			try {
				return OBJECT_MAPPER.readValue(json, type);
			} catch (JsonProcessingException jacksonEx) {
				// Jackson 也失败，以 FastJSON2 异常为根因抛出
				throw new IllegalStateException("Conversion from JSON to " + type.getName() + " failed", ex);
			}
		}
	}

	/**
	 * 将 JSON 字符串反序列化为指定的 Java 类型（反射 {@link Type} 方式）。
	 * <p>
	 * 支持泛型类型（如 {@code List<String>}、{@code Map<String, Object>}）的反序列化，
	 * 优先使用 FastJSON2，失败后降级到 Jackson。
	 *
	 * @param <T>  目标类型
	 * @param json 待解析的 JSON 字符串，不能为 {@code null}
	 * @param type 目标 Java 反射类型，不能为 {@code null}
	 * @return 反序列化得到的 Java 对象
	 * @throws IllegalArgumentException 若 {@code json} 或 {@code type} 为 {@code null}
	 * @throws IllegalStateException    若 FastJSON2 和 Jackson 均解析失败
	 */
	public static <T> T fromJson(String json, Type type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			// 优先使用 FastJSON2 解析
			return JSON.parseObject(json, type);
		}
		catch (Exception ex) {
			// FastJSON2 失败，降级到 Jackson，通过 constructType 将 java.lang.reflect.Type 转换为 Jackson 类型描述符
			try {
				return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.constructType(type));
			} catch (JsonProcessingException jacksonEx) {
				throw new IllegalStateException("Conversion from JSON to " + type.getTypeName() + " failed", ex);
			}
		}
	}

	/**
	 * 将 JSON 字符串反序列化为 Jackson {@link TypeReference} 描述的泛型类型。
	 * <p>
	 * 适用于需要完整保留泛型信息的场景，例如：
	 * <pre>{@code
	 * List<Map<String, Object>> result = JsonParser.fromJson(json,
	 *     new TypeReference<List<Map<String, Object>>>() {});
	 * }</pre>
	 * 此方法仅使用 Jackson 解析，不使用 FastJSON2 作为主路径。
	 *
	 * @param <T>  目标泛型类型
	 * @param json 待解析的 JSON 字符串，不能为 {@code null}
	 * @param type Jackson TypeReference，不能为 {@code null}
	 * @return 反序列化得到的 Java 对象
	 * @throws IllegalArgumentException 若 {@code json} 或 {@code type} 为 {@code null}
	 * @throws IllegalStateException    若 Jackson 解析失败
	 */
	public static <T> T fromJson(String json, TypeReference<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			// 使用 Jackson TypeReference 解析，可完整保留泛型信息
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from JSON to " + type.getType().getTypeName() + " failed",
					ex);
		}
	}

	/**
	 * 将 Java 对象序列化为 JSON 字符串。
	 * <p>
	 * 序列化策略（按优先级）：
	 * <ol>
	 *   <li>若 {@code object} 为 {@code null}，直接返回字符串 {@code "null"}。</li>
	 *   <li>若 {@link #JSON_FILTERS} 为空，使用 FastJSON2 默认序列化。</li>
	 *   <li>若 {@link #JSON_FILTERS} 不为空，使用 FastJSON2 携带全部过滤器序列化（用于字段脱敏等场景）。</li>
	 *   <li>若 FastJSON2 序列化抛出异常，记录 warn 日志，降级使用 Jackson 序列化。</li>
	 *   <li>若 Jackson 也失败，记录 error 日志，返回 {@code {"error":"Serialization failed"}}。</li>
	 * </ol>
	 * 若 FastJSON2 或 Jackson 序列化结果为 {@code null}（极少数情况），返回 {@code "{}"} 作为安全兜底。
	 *
	 * @param object 待序列化的 Java 对象，为 {@code null} 时返回 {@code "null"}
	 * @return JSON 字符串，不会为 {@code null}
	 */
	public static String toJson(Object object) {
		if (object == null) {
			// null 对象对应 JSON 的 null 值
			return "null";
		}

		try {
			String result;
			if (JSON_FILTERS.isEmpty()) {
				// 无自定义过滤器：使用 FastJSON2 默认序列化
				result = JSON.toJSONString(object);
			} else {
				// 有自定义过滤器：携带所有过滤器进行序列化，实现字段脱敏等能力
				result = JSON.toJSONString(object, JSON_FILTERS.toArray(new ValueFilter[0]));
			}
			// FastJSON2 序列化结果为 null 时（极少数情况）兜底返回空对象
			return (result != null) ? result : "{}";
		}
		catch (Exception ex) {
			// FastJSON2 序列化失败，记录警告并降级到 Jackson
			logger.warn("FastJSON2 with MCP filter serialization failed for {}, falling back to Jackson: {}",
				object.getClass().getSimpleName(), ex.getMessage());
			try {
				String result = OBJECT_MAPPER.writeValueAsString(object);
				return (result != null) ? result : "{}";
			} catch (JsonProcessingException jacksonEx) {
				// FastJSON2 和 Jackson 均失败，返回固定的错误 JSON，避免抛出异常中断调用链
				logger.error("Both FastJSON2 and Jackson serialization failed", ex);
				return "{\"error\":\"Serialization failed\"}";
			}
		}
	}

	/**
	 * 将任意值转换为指定 Java 类型的强类型对象。
	 * <p>
	 * 主要用于将 MCP Tool 调用时传入的参数值（通常为 JSON 解析后的基本类型或 Map）
	 * 转换为 Tool 方法形参所声明的 Java 类型。
	 * <p>
	 * 转换逻辑（按优先级）：
	 * <ol>
	 *   <li>若 {@code type} 为基本类型，先通过 {@link #resolvePrimitiveIfNecessary} 转换为对应包装类。</li>
	 *   <li>{@code String}：调用 {@code value.toString()} 转换。</li>
	 *   <li>{@code Byte}：调用 {@code Byte.parseByte(value.toString())} 转换。</li>
	 *   <li>{@code Integer}：先转为 {@link BigDecimal} 再调用 {@code intValueExact()}，
	 *       防止精度丢失或溢出（会在溢出时抛出 {@link ArithmeticException}）。</li>
	 *   <li>{@code Short}：调用 {@code Short.parseShort(value.toString())} 转换。</li>
	 *   <li>{@code Long}：先转为 {@link BigDecimal} 再调用 {@code longValueExact()}，防止溢出。</li>
	 *   <li>{@code Double}：调用 {@code Double.parseDouble(value.toString())} 转换。</li>
	 *   <li>{@code Float}：调用 {@code Float.parseFloat(value.toString())} 转换。</li>
	 *   <li>{@code Boolean}：调用 {@code Boolean.parseBoolean(value.toString())} 转换。</li>
	 *   <li>{@code Character}：要求 {@code value.toString()} 长度恰好为 1，否则抛出 {@link IllegalArgumentException}。</li>
	 *   <li>枚举类型：调用 {@code Enum.valueOf} 按名称匹配枚举常量。</li>
	 *   <li>其他复杂类型：先将 {@code value} 序列化为 JSON 字符串，再反序列化为目标类型。</li>
	 * </ol>
	 *
	 * @param value 待转换的原始值，不能为 {@code null}
	 * @param type  目标 Java 类型，不能为 {@code null}
	 * @return 转换后的强类型对象
	 * @throws IllegalArgumentException 若 {@code value} 或 {@code type} 为 {@code null}，
	 *                                  或字符类型转换时 {@code value} 长度不为 1
	 * @throws ArithmeticException      若整数/长整数转换时发生精度溢出
	 */
	public static Object toTypedObject(Object value, Class<?> type) {
		if (value == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}

		// 若目标类型是基本类型（如 int、boolean），先解析为对应包装类（如 Integer、Boolean）
		Class<?> javaType = resolvePrimitiveIfNecessary(type);

		if (javaType == String.class) {
			// 字符串类型：直接调用 toString()
			return value.toString();
		}
		else if (javaType == Byte.class) {
			// 字节类型：字符串解析为 Byte
			return Byte.parseByte(value.toString());
		}
		else if (javaType == Integer.class) {
			// 整数类型：通过 BigDecimal 中间转换，防止浮点精度问题导致 intValue 不准确
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.intValueExact();
		}
		else if (javaType == Short.class) {
			// 短整数类型：字符串解析为 Short
			return Short.parseShort(value.toString());
		}
		else if (javaType == Long.class) {
			// 长整数类型：通过 BigDecimal 中间转换，防止精度丢失
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.longValueExact();
		}
		else if (javaType == Double.class) {
			// 双精度浮点数类型：字符串解析为 Double
			return Double.parseDouble(value.toString());
		}
		else if (javaType == Float.class) {
			// 单精度浮点数类型：字符串解析为 Float
			return Float.parseFloat(value.toString());
		}
		else if (javaType == Boolean.class) {
			// 布尔类型：字符串解析为 Boolean（"true" -> true，其他 -> false）
			return Boolean.parseBoolean(value.toString());
		}
		else if (javaType == Character.class) {
			// 字符类型：要求字符串长度恰好为 1
			String s = value.toString();
			if (s.length() == 1) {
				return s.charAt(0);
			}
			throw new IllegalArgumentException("Cannot convert to char: " + value);
		}
		else if (javaType.isEnum()) {
			// 枚举类型：按名称匹配枚举常量
			@SuppressWarnings("unchecked")
			Class<Enum> enumType = (Class<Enum>) javaType;
			return Enum.valueOf(enumType, value.toString());
		}

		// 复杂对象类型：先序列化为 JSON，再反序列化为目标类型（支持 Map -> POJO 等场景）
		String json = JsonParser.toJson(value);
		return JsonParser.fromJson(json, javaType);
	}

	/**
	 * 若目标类型为 Java 基本类型，则返回其对应的包装类；否则直接返回原类型。
	 * <p>
	 * 例如：{@code int.class} → {@code Integer.class}，{@code boolean.class} → {@code Boolean.class}。
	 * 用于统一后续的类型判断逻辑，避免在每个分支中重复处理基本类型与包装类的双重判断。
	 *
	 * @param type 待处理的 Java 类型
	 * @return 若为基本类型则返回对应包装类，否则返回原类型
	 */
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> type) {
		if (type.isPrimitive()) {
			// 委托给 Utils 工具类完成基本类型到包装类的映射
			return Utils.getWrapperClassForPrimitive(type);
		}
		return type;
	}

}
