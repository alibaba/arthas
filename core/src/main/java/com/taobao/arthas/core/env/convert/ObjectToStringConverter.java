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
 * Simply calls {@link Object#toString()} to convert a source Object to a String.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ObjectToStringConverter implements Converter<Object, String> {

	public String convert(Object source, Class<String> targetType) {
		return source.toString();
	}

}
