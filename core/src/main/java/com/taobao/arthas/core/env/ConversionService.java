/*
 * Copyright 2002-2016 the original author or authors.
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

package com.taobao.arthas.core.env;

/**
 * 类型转换服务接口
 * <p>
 * 这是类型转换系统的入口点。调用 {@link #convert(Object, Class)} 方法
 * 可以使用该系统执行线程安全的类型转换
 * <p>
 * 该接口提供了以下核心功能：
 * <ul>
 * <li>检查是否可以从源类型转换为目标类型</li>
 * <li>执行实际的类型转换操作</li>
 * <li>支持多种数据类型之间的转换</li>
 * </ul>
 * <p>
 * 注意：对于集合、数组和 Map 类型的转换，即使基础元素不可转换，
 * canConvert 方法也可能返回 true。调用者需要处理这种异常情况
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
public interface ConversionService {

    /**
     * 检查是否可以将 {@code sourceType} 类型的对象转换为 {@code targetType}
     * <p>
     * 如果该方法返回 {@code true}，表示 {@link #convert(Object, Class)}
     * 方法能够将 {@code sourceType} 的实例转换为 {@code targetType}
     * <p>
     * 关于集合、数组和 Map 类型的特别说明：
     * 对于集合、数组和 Map 类型之间的转换，即使基础元素不可转换，
     * 该方法也可能返回 {@code true}，因为转换调用可能会生成 {@link ConversionException}
     * 调用者在处理集合和 Map 时需要处理这种异常情况
     *
     * @param sourceType 要转换的源类型（如果源为 {@code null}，可能为 {@code null}）
     * @param targetType 要转换的目标类型（必需）
     * @return 如果可以执行转换返回 {@code true}，否则返回 {@code false}
     * @throws IllegalArgumentException 如果 {@code targetType} 为 {@code null}
     */
    boolean canConvert(Class<?> sourceType, Class<?> targetType);

    /**
     * 将给定的 {@code source} 对象转换为指定的 {@code targetType}
     * <p>
     * 该方法执行实际的类型转换操作
     * 如果转换失败，将抛出 ConversionException 异常
     * 如果目标类型为 null，将抛出 IllegalArgumentException 异常
     *
     * @param source     要转换的源对象（可能为 {@code null}）
     * @param targetType 要转换的目标类型（必需）
     * @param <T>        目标类型的泛型参数
     * @return 转换后的对象，是 targetType 的实例
     * @throws ConversionException      如果发生转换异常
     * @throws IllegalArgumentException 如果 targetType 为 {@code null}
     */
    <T> T convert(Object source, Class<T> targetType);

}
