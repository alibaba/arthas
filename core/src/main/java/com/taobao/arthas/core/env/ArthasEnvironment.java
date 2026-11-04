package com.taobao.arthas.core.env;

import java.security.AccessControlException;
import java.util.Map;

/**
 * 
 * @author hengyunabc 2019-12-27
 *
 */
public class ArthasEnvironment implements Environment {
    /** System environment property source name: {@value}. */
    public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

    /** JVM system properties property source name: {@value}. */
    public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";

    private final MutablePropertySources propertySources = new MutablePropertySources();

    private final ConfigurablePropertyResolver propertyResolver = new PropertySourcesPropertyResolver(
            this.propertySources);

    public ArthasEnvironment() {
        propertySources.addLast(
                new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
        propertySources
                .addLast(new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
    }

    /**
     * Add the given property source object with highest precedence.
     */
    public void addFirst(PropertySource<?> propertySource) {
        this.propertySources.addFirst(propertySource);
    }

    /**
     * Add the given property source object with lowest precedence.
     */
    public void addLast(PropertySource<?> propertySource) {
        this.propertySources.addLast(propertySource);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, Object> getSystemProperties() {
        try {
            return (Map) System.getProperties();
        } catch (AccessControlException ex) {
            return (Map) new ReadOnlySystemAttributesMap() {
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, Object> getSystemEnvironment() {
        try {
            return (Map) System.getenv();
        } catch (AccessControlException ex) {
            return (Map) new ReadOnlySystemAttributesMap() {
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
    // Implementation of PropertyResolver interface
    // ---------------------------------------------------------------------

    @Override
    public boolean containsProperty(String key) {
        return this.propertyResolver.containsProperty(key);
    }

    @Override
    public String getProperty(String key) {
        return this.propertyResolver.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return this.propertyResolver.getProperty(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return this.propertyResolver.getProperty(key, targetType);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return this.propertyResolver.getProperty(key, targetType, defaultValue);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        return this.propertyResolver.getRequiredProperty(key);
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        return this.propertyResolver.getRequiredProperty(key, targetType);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return this.propertyResolver.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return this.propertyResolver.resolveRequiredPlaceholders(text);
    }

}
