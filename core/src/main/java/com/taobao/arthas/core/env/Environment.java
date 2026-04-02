package com.taobao.arthas.core.env;

/**
 * 环境接口
 *
 * 表示当前应用程序运行环境的接口，提供属性解析能力。
 * 继承自 {@link PropertyResolver}，因此可以解析属性值。
 *
 * <p>
 * 在 Arthas 中，环境用于集中管理各种配置属性，包括：
 * <ul>
 * <li>系统属性</li>
 * <li>环境变量</li>
 * <li>配置文件属性</li>
 * <li>命令行参数</li>
 * </ul>
 *
 * <p>
 * 此接口是 Arthas 配置系统的核心接口之一，允许统一访问不同来源的配置属性。
 *
 * @author Arthas
 * @see PropertyResolver
 */
public interface Environment extends PropertyResolver {

}
