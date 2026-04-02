/*
 * Copyright 2002-2011 the original author or authors.
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

package com.taobao.arthas.core.env.convert;

import com.taobao.arthas.core.env.ConversionService;

/**
 * 可配置的类型转换服务接口
 *
 * 这是一个配置接口，由大多数（如果不是全部）{@link ConversionService} 类型实现。
 * 它整合了 {@link ConversionService} 暴露的只读操作和 {@link ConverterRegistry} 的可变操作，
 * 允许方便地临时添加和删除 {@link org.springframework.core.convert.converter.Converter 转换器}。
 *
 * 后者在应用程序上下文引导代码中处理 {@link org.springframework.core.env.ConfigurableEnvironment
 * ConfigurableEnvironment} 实例时特别有用。
 *
 * <p>此接口的主要作用：</p>
 * <ul>
 *   <li>扩展了 ConversionService 接口，继承类型转换的基本功能</li>
 *   <li>允许动态添加和移除转换器</li>
 *   <li>提供了更灵活的类型转换配置能力</li>
 * </ul>
 *
 * @author Chris Beams
 * @since 3.1
 * @see com.taobao.arthas.core.env.springframework.core.env.ConfigurablePropertyResolver#getConversionService()
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface ConfigurableConversionService extends ConversionService {

}
