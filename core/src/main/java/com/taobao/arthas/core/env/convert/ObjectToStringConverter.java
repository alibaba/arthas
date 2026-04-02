/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taobao.arthas.core.env.convert;

/**
 * 对象到字符串的转换器
 *
 * 该转换器通过简单调用 {@link Object#toString()} 方法将源对象转换为字符串。
 * 这是一个简单直接的转换实现，不涉及复杂的转换逻辑。
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ObjectToStringConverter implements Converter<Object, String> {

	/**
	 * 将源对象转换为目标类型（字符串）
	 *
	 * 该方法直接调用源对象的 toString() 方法获取字符串表示。
	 * 这是一个简单高效的转换方式，适用于任何实现了 toString() 方法的对象。
	 *
	 * @param source 源对象，需要被转换为字符串的对象
	 * @param targetType 目标类型，本方法中为 String 类型
	 * @return 源对象的字符串表示形式
	 */
	public String convert(Object source, Class<String> targetType) {
		return source.toString();
	}

}
