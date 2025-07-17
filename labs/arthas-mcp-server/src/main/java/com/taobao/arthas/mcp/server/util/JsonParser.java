package com.taobao.arthas.mcp.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * Utilities to perform parsing operations between JSON and Java.
 */
public final class JsonParser {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		.build();

	private JsonParser() {
	}

	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	public static <T> T fromJson(String json, Class<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from JSON to "+ type.getName() +" failed", ex);
		}
	}

	public static <T> T fromJson(String json, Type type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.constructType(type));
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from JSON to "+ type.getTypeName() +" failed", ex);
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
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from Object to JSON failed", ex);
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

