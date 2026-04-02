/*
 * Copyright 2002-2015 the original author or authors.
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
 * 类型转换器接口
 *
 * 用于将源类型 S 的对象转换为目标类型 T 的对象。
 * 此接口的实现是线程安全的，可以在多个线程之间共享。
 *
 * <p>转换器的特点：</p>
 * <ul>
 *   <li>支持泛型：可以指定源类型 S 和目标类型 T</li>
 *   <li>线程安全：实现类必须是线程安全的，可以被多个线程共享使用</li>
 *   <li>灵活性：可以根据需要实现条件转换逻辑</li>
 * </ul>
 *
 * <p>实现类还可以选择实现 {@link ConditionalConverter} 接口来支持条件转换。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>字符串到基本类型的转换（如 String -> Integer）</li>
 *   <li>对象之间的类型转换</li>
 *   <li>集合和数组类型的转换</li>
 * </ul>
 *
 * @author Keith Donald
 * @since 3.0
 * @param <S> 源类型，即被转换对象的类型
 * @param <T> 目标类型，即转换后对象的类型
 */
public interface Converter<S, T> {

	/**
	 * 将源类型 S 的对象转换为目标类型 T
	 *
	 * <p>此方法执行实际的类型转换操作。实现类需要处理：</p>
	 * <ul>
	 *   <li>源对象到目标对象的转换逻辑</li>
	 *   <li>可能的格式验证和异常处理</li>
	 *   <li>null 值的处理策略</li>
	 * </ul>
	 *
	 * @param source 要转换的源对象，必须是 S 类型的实例（永远不为 {@code null}）
	 * @param targetType 目标类型的 Class 对象，用于确定具体的转换目标类型
	 * @return 转换后的对象，必须是 T 类型的实例（可能为 {@code null}）
	 * @throws IllegalArgumentException 如果源对象无法转换为目标类型
	 */
	T convert(S source, Class<T> targetType);

}
