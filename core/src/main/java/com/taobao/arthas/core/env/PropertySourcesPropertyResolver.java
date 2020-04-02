/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * {@link PropertyResolver} implementation that resolves property values against
 * an underlying set of {@link PropertySources}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

    private final PropertySources propertySources;

    /**
     * Create a new resolver against the given property sources.
     * 
     * @param propertySources the set of {@link PropertySource} objects to use
     */
    public PropertySourcesPropertyResolver(PropertySources propertySources) {
        this.propertySources = propertySources;
    }

    @Override
    public boolean containsProperty(String key) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (propertySource.containsProperty(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class, true);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetValueType) {
        return getProperty(key, targetValueType, true);
    }

    @Override
    protected String getPropertyAsRawString(String key) {
        return getProperty(key, String.class, false);
    }

//	protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
//		if (this.propertySources != null) {
//			for (PropertySource<?> propertySource : this.propertySources) {
//				Object value = propertySource.getProperty(key);
//				if (value != null) {
//					if (resolveNestedPlaceholders && value instanceof String) {
//						value = resolveNestedPlaceholders((String) value);
//					}
//					logKeyFound(key, propertySource, value);
//					return convertValueIfNecessary(value, targetValueType);
//				}
//			}
//		}
//		return null;
//	}

    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                Object value;
                if ((value = propertySource.getProperty(key)) != null) {
                    Class<?> valueType = value.getClass();
                    if (resolveNestedPlaceholders && value instanceof String) {
                        value = resolveNestedPlaceholders((String) value);
                    }
                    if (!this.conversionService.canConvert(valueType, targetValueType)) {
                        throw new IllegalArgumentException(
                                String.format("Cannot convert value [%s] from source type [%s] to target type [%s]",
                                        value, valueType.getSimpleName(), targetValueType.getSimpleName()));
                    }
                    return this.conversionService.convert(value, targetValueType);
                }
            }
        }
        return null;
    }

    /**
     * Log the given key as found in the given {@link PropertySource}, resulting in
     * the given value.
     * <p>
     * The default implementation writes a debug log message with key and source. As
     * of 4.3.3, this does not log the value anymore in order to avoid accidental
     * logging of sensitive settings. Subclasses may override this method to change
     * the log level and/or log message, including the property's value if desired.
     * 
     * @param key            the key found
     * @param propertySource the {@code PropertySource} that the key has been found
     *                       in
     * @param value          the corresponding value
     * @since 4.3.1
     */
    protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
//		if (logger.isDebugEnabled()) {
//			logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
//					"' with value of type " + value.getClass().getSimpleName());
//		}
    }

}
