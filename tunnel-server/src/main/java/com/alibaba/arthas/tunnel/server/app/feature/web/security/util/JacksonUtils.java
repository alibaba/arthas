/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.arthas.tunnel.server.app.feature.web.security.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson 工具集
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.7
 */
@UtilityClass
public class JacksonUtils {

	private static final ObjectMapper defaultObjectMapper = new DefaultObjectMapper();

	public static <T> String toJSONString(T object) {
		return toJSONString(object, Include.USE_DEFAULTS);
	}

	public static <T> String toJSONString(T object, Include include) {
		return toJSONString(object, include, defaultObjectMapper);
	}

	public static <T> String toJSONString(T object, Include include, ObjectMapper objectMapper) {
		if (objectMapper == null) {
			objectMapper = defaultObjectMapper;
		}
		try {
			return getObjectWriter(include, objectMapper).writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new JacksonException(e);
		}
	}

	public static <T> T parseObject(String text, Class<T> cls, ObjectMapper objectMapper) {
		try {
			return objectMapper.readValue(text, cls);
		} catch (IOException e) {
			throw new JacksonException(e);
		}
	}

	public static <T> T parseObject(String text, Class<T> cls) {
		return parseObject(text, cls, defaultObjectMapper);
	}

	public static <K, V, T> T parseObject(Map<K, V> map, Class<T> cls, ObjectMapper objectMapper) {
		return objectMapper.convertValue(map, cls);
	}

	public static <K, V, T> T parseObject(Map<K, V> map, Class<T> cls) {
		return parseObject(map, cls, defaultObjectMapper);
	}

	private static ObjectWriter getObjectWriter(Include include, ObjectMapper objectMapper) {
		return objectMapper.setSerializationInclusion(include).writer();
	}

	public static class DefaultObjectMapper extends ObjectMapper {

		private static final long serialVersionUID = 8090216975101285238L;

		public DefaultObjectMapper() {
			super();

			// 序列化时忽略NULL
			this.setSerializationInclusion(Include.NON_NULL);
			// 反序列化时忽略未知属性
			this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			// 禁用空对象转换校验
			this.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		}
	}
}
