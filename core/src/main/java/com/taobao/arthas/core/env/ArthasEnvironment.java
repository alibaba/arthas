package com.taobao.arthas.core.env;

import java.security.AccessControlException;
import java.util.Map;

/**
 * Arthas 环境配置类
 * <p>
 * 该类实现了 Environment 接口，提供了访问系统环境和 JVM 系统属性的能力
 * 支持动态添加属性源，并通过委托模式使用 PropertyResolver 进行属性解析
 *
 * @author hengyunabc 2019-12-27
 */
public class ArthasEnvironment implements Environment {
    /** 系统环境属性源的名称：{@value} */
    public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

    /** JVM 系统属性属性源的名称：{@value} */
    public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";

    /**
     * 可变的属性源集合
     * 用于管理多个属性源，支持动态添加和调整优先级
     */
    private final MutablePropertySources propertySources = new MutablePropertySources();

    /**
     * 可配置的属性解析器
     * 通过委托给 PropertySourcesPropertyResolver 来解析属性值
     */
    private final ConfigurablePropertyResolver propertyResolver = new PropertySourcesPropertyResolver(
            this.propertySources);

    /**
     * 构造函数
     * <p>
     * 初始化时自动添加系统环境变量和 JVM 系统属性作为属性源
     * 系统环境变量优先级较高（addFirst），JVM 系统属性优先级较低（addLast）
     */
    public ArthasEnvironment() {
        // 添加系统环境变量属性源（优先级高）
        propertySources.addLast(
                new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
        // 添加 JVM 系统属性属性源（优先级低）
        propertySources
                .addLast(new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
    }

    /**
     * 将给定的属性源对象添加到属性源列表的开头（最高优先级）
     * <p>
     * 当查找属性时，优先级高的属性源会被优先搜索
     *
     * @param propertySource 要添加的属性源对象
     */
    public void addFirst(PropertySource<?> propertySource) {
        this.propertySources.addFirst(propertySource);
    }

    /**
     * 将给定的属性源对象添加到属性源列表的末尾（最低优先级）
     * <p>
     * 当查找属性时，优先级低的属性源会被最后搜索
     *
     * @param propertySource 要添加的属性源对象
     */
    public void addLast(PropertySource<?> propertySource) {
        this.propertySources.addLast(propertySource);
    }

    /**
     * 获取 JVM 系统属性
     * <p>
     * 如果安全管理器不允许访问，则返回一个只读的 Map 视图
     *
     * @return 包含所有 JVM 系统属性的 Map
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, Object> getSystemProperties() {
        try {
            // 尝试直接获取系统属性
            return (Map) System.getProperties();
        } catch (AccessControlException ex) {
            // 如果安全管理器不允许访问，返回一个只读的 Map 视图
            return (Map) new ReadOnlySystemAttributesMap() {
                /**
                 * 获取单个系统属性
                 *
                 * @param attributeName 属性名称
                 * @return 属性值，如果不允许访问返回 null
                 */
                @Override
                protected String getSystemAttribute(String attributeName) {
                    try {
                        return System.getProperty(attributeName);
                    } catch (AccessControlException ex) {
                        return null;
                    }
                }
            };
        }
    }

    /**
     * 获取系统环境变量
     * <p>
     * 如果安全管理器不允许访问，则返回一个只读的 Map 视图
     *
     * @return 包含所有系统环境变量的 Map
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, Object> getSystemEnvironment() {
        try {
            // 尝试直接获取环境变量
            return (Map) System.getenv();
        } catch (AccessControlException ex) {
            // 如果安全管理器不允许访问，返回一个只读的 Map 视图
            return (Map) new ReadOnlySystemAttributesMap() {
                /**
                 * 获取单个环境变量
                 *
                 * @param attributeName 环境变量名称
                 * @return 环境变量值，如果不允许访问返回 null
                 */
                @Override
                protected String getSystemAttribute(String attributeName) {
                    try {
                        return System.getenv(attributeName);
                    } catch (AccessControlException ex) {
                        return null;
                    }
                }
            };
        }
    }

    // ---------------------------------------------------------------------
    // PropertyResolver 接口的实现
    // ---------------------------------------------------------------------

    /**
     * 检查是否包含指定的属性
     *
     * @param key 要检查的属性名称
     * @return 如果属性存在返回 true，否则返回 false
     */
    @Override
    public boolean containsProperty(String key) {
        return this.propertyResolver.containsProperty(key);
    }

    /**
     * 获取指定属性的字符串值
     *
     * @param key 属性名称
     * @return 属性值，如果不存在返回 null
     */
    @Override
    public String getProperty(String key) {
        return this.propertyResolver.getProperty(key);
    }

    /**
     * 获取指定属性的字符串值，如果不存在则返回默认值
     *
     * @param key          属性名称
     * @param defaultValue 默认值
     * @return 属性值，如果不存在返回指定的默认值
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        return this.propertyResolver.getProperty(key, defaultValue);
    }

    /**
     * 获取指定属性的值，并转换为目标类型
     *
     * @param key        属性名称
     * @param targetType 目标类型
     * @param <T>        目标类型的泛型参数
     * @return 属性值（转换为目标类型），如果不存在返回 null
     */
    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return this.propertyResolver.getProperty(key, targetType);
    }

    /**
     * 获取指定属性的值，并转换为目标类型，如果不存在则返回默认值
     *
     * @param key          属性名称
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型的泛型参数
     * @return 属性值（转换为目标类型），如果不存在返回指定的默认值
     */
    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return this.propertyResolver.getProperty(key, targetType, defaultValue);
    }

    /**
     * 获取必需的属性值（字符串类型）
     * <p>
     * 如果属性不存在，抛出 IllegalStateException 异常
     *
     * @param key 属性名称
     * @return 属性值
     * @throws IllegalStateException 如果属性不存在
     */
    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        return this.propertyResolver.getRequiredProperty(key);
    }

    /**
     * 获取必需的属性值，并转换为目标类型
     * <p>
     * 如果属性不存在，抛出 IllegalStateException 异常
     *
     * @param key       属性名称
     * @param targetType 目标类型
     * @param <T>       目标类型的泛型参数
     * @return 属性值（转换为目标类型）
     * @throws IllegalStateException 如果属性不存在
     */
    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        return this.propertyResolver.getRequiredProperty(key, targetType);
    }

    /**
     * 解析文本中的占位符（非严格模式）
     * <p>
     * 无法解析的占位符将保留原样，不会抛出异常
     *
     * @param text 包含占位符的文本
     * @return 解析后的文本
     */
    @Override
    public String resolvePlaceholders(String text) {
        return this.propertyResolver.resolvePlaceholders(text);
    }

    /**
     * 解析文本中的占位符（严格模式）
     * <p>
     * 如果有无法解析的占位符，将抛出 IllegalArgumentException 异常
     *
     * @param text 包含占位符的文本
     * @return 解析后的文本
     * @throws IllegalArgumentException 如果有无法解析的占位符
     */
    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return this.propertyResolver.resolveRequiredPlaceholders(text);
    }

}
