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

/**
 * Utilities to perform parsing operations between JSON and Java.
 */
public final class JsonParser {

	private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);
	private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
	private static final ValueFilter[] JSON_FILTERS = new ValueFilter[] { new McpObjectVOFilter() };


	private static ObjectMapper createObjectMapper() {
		return JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.addModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();
	}

	private JsonParser() {
	}

	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	public static <T> T fromJson(String json, Class<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return JSON.parseObject(json, type);
		}
		catch (Exception ex) {
			try {
				return OBJECT_MAPPER.readValue(json, type);
			} catch (JsonProcessingException jacksonEx) {
				throw new IllegalStateException("Conversion from JSON to " + type.getName() + " failed", ex);
			}
		}
	}

	public static <T> T fromJson(String json, Type type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return JSON.parseObject(json, type);
		}
		catch (Exception ex) {
			try {
				return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.constructType(type));
			} catch (JsonProcessingException jacksonEx) {
				throw new IllegalStateException("Conversion from JSON to " + type.getTypeName() + " failed", ex);
			}
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from JSON to " + type.getType().getTypeName() + " failed",
					ex);
		}
	}

	/**
	 * Converts a Java object to a JSON string.
	 */
	public static String toJson(Object object) {
		if (object == null) {
			return "null";
		}

		try {
//			JSONWriter.Context context = JSONFactory.createWriteContext();
//			context.setMaxLevel(10); // 大幅降低最大深度，避免深层递归
//			context.config(JSONWriter.Feature.IgnoreErrorGetter, true);
//			context.config(JSONWriter.Feature.ReferenceDetection, true);
//			context.config(JSONWriter.Feature.IgnoreNonFieldGetter, true);
//			context.config(JSONWriter.Feature.WriteNonStringKeyAsString, true);

			String result = JSON.toJSONString(object, JSON_FILTERS);
			return (result != null) ? result : "{}";
		}
		catch (Exception ex) {
			logger.warn("FastJSON2 with MCP filter serialization failed for {}, falling back to Jackson: {}",
				object.getClass().getSimpleName(), ex.getMessage());
			try {
				String result = OBJECT_MAPPER.writeValueAsString(object);
				return (result != null) ? result : "{}";
			} catch (JsonProcessingException jacksonEx) {
				logger.error("Both FastJSON2 and Jackson serialization failed", ex);
				return "{\"error\":\"Serialization failed\"}";
			}
		}
	}

	public static Object toTypedObject(Object value, Class<?> type) {
		if (value == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}

		Class<?> javaType = resolvePrimitiveIfNecessary(type);

		if (javaType == String.class) {
			return value.toString();
		}
		else if (javaType == Byte.class) {
			return Byte.parseByte(value.toString());
		}
		else if (javaType == Integer.class) {
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.intValueExact();
		}
		else if (javaType == Short.class) {
			return Short.parseShort(value.toString());
		}
		else if (javaType == Long.class) {
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.longValueExact();
		}
		else if (javaType == Double.class) {
			return Double.parseDouble(value.toString());
		}
		else if (javaType == Float.class) {
			return Float.parseFloat(value.toString());
		}
		else if (javaType == Boolean.class) {
			return Boolean.parseBoolean(value.toString());
		}
		else if (javaType == Character.class) {
			String s = value.toString();
			if (s.length() == 1) {
				return s.charAt(0);
			}
			throw new IllegalArgumentException("Cannot convert to char: " + value);
		}
		else if (javaType.isEnum()) {
			@SuppressWarnings("unchecked")
			Class<Enum> enumType = (Class<Enum>) javaType;
			return Enum.valueOf(enumType, value.toString());
		}


		String json = JsonParser.toJson(value);
		return JsonParser.fromJson(json, javaType);
	}

	public static Class<?> resolvePrimitiveIfNecessary(Class<?> type) {
		if (type.isPrimitive()) {
			return Utils.getWrapperClassForPrimitive(type);
		}
		return type;
	}

}

