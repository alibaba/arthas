/*
 * Copyright 2002-2017 the original author or authors.
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

package com.taobao.arthas.core.env;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 缺失必需属性异常
 *
 * 当找不到所需的属性时抛出的异常。
 *
 * <p>
 * 此异常用于表示在属性验证过程中，某些被标记为必需的属性无法被解析。
 * 它会收集所有缺失的必需属性，并在异常消息中列出，便于用户了解
 * 哪些配置缺失。
 *
 * <p>
 * 典型使用场景：
 * <ul>
 * <li>应用程序启动时验证关键配置</li>
 * <li>检查必需的系统属性是否设置</li>
 * <li>确保关键的环境变量已定义</li>
 * </ul>
 *
 * @author Chris Beams
 * @since 3.1
 * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
 * @see ConfigurablePropertyResolver#validateRequiredProperties()
 * @see org.springframework.context.support.AbstractApplicationContext#prepareRefresh()
 */
@SuppressWarnings("serial")
public class MissingRequiredPropertiesException extends IllegalStateException {

    /**
     * 缺失的必需属性集合
     *
     * 使用 LinkedHashSet 保持插入顺序，确保属性按照添加顺序显示
     */
    private final Set<String> missingRequiredProperties = new LinkedHashSet<String>();

    /**
     * 添加一个缺失的必需属性到集合中
     *
     * <p>
     * 此方法由属性解析器在验证过程中调用，用于记录所有缺失的必需属性。
     * 使用 Set 集合避免重复添加相同的属性。
     *
     * @param key 缺失的属性名称
     */
    void addMissingRequiredProperty(String key) {
        // 将缺失的属性名添加到集合中
        this.missingRequiredProperties.add(key);
    }

    /**
     * 获取异常的详细消息
     *
     * <p>
     * 此方法返回一个描述性的错误消息，列出所有缺失的必需属性。
     * 消息格式为："The following properties were declared as required
     * but could not be resolved: [属性列表]"
     *
     * @return 异常的详细消息
     */
    @Override
    public String getMessage() {
        // 构建包含所有缺失属性的错误消息
        return "The following properties were declared as required but could not be resolved: "
                + getMissingRequiredProperties();
    }

    /**
     * 返回被标记为必需但在验证时缺失的属性集合
     *
     * <p>
     * 此方法返回一个不可修改的视图（虽然实现返回的是原始集合），
     * 包含所有在验证过程中发现的缺失必需属性。
     *
     * <p>
     * 使用此方法可以：
     * <ul>
     * <li>获取详细的缺失属性列表</li>
     * <li>在日志中记录具体的缺失配置</li>
     * <li>为用户提供更友好的错误提示</li>
     * </ul>
     *
     * @return 缺失的必需属性名称集合，保持插入顺序
     * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
     * @see ConfigurablePropertyResolver#validateRequiredProperties()
     */
    public Set<String> getMissingRequiredProperties() {
        // 返回缺失属性的集合
        return this.missingRequiredProperties;
    }

}
