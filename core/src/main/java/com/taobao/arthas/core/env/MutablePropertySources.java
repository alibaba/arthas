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

package com.taobao.arthas.core.env;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Default implementation of the {@link PropertySources} interface. Allows
 * manipulation of contained property sources and provides a constructor for
 * copying an existing {@code PropertySources} instance.
 *
 * <p>
 * Where <em>precedence</em> is mentioned in methods such as {@link #addFirst}
 * and {@link #addLast}, this is with regard to the order in which property
 * sources will be searched when resolving a given property with a
 * {@link PropertyResolver}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertySourcesPropertyResolver
 */
public class MutablePropertySources implements PropertySources {

    static final String NON_EXISTENT_PROPERTY_SOURCE_MESSAGE = "PropertySource named [%s] does not exist";
    static final String ILLEGAL_RELATIVE_ADDITION_MESSAGE = "PropertySource named [%s] cannot be added relative to itself";

    private final LinkedList<PropertySource<?>> propertySourceList = new LinkedList<PropertySource<?>>();

    /**
     * Create a new {@link MutablePropertySources} object.
     */
    public MutablePropertySources() {
    }

    /**
     * Create a new {@code MutablePropertySources} from the given propertySources
     * object, preserving the original order of contained {@code PropertySource}
     * objects.
     */
    public MutablePropertySources(PropertySources propertySources) {
        this();
        for (PropertySource<?> propertySource : propertySources) {
            this.addLast(propertySource);
        }
    }

    public boolean contains(String name) {
        return this.propertySourceList.contains(PropertySource.named(name));
    }

    public PropertySource<?> get(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        return index == -1 ? null : this.propertySourceList.get(index);
    }

    public Iterator<PropertySource<?>> iterator() {
        return this.propertySourceList.iterator();
    }

    /**
     * Add the given property source object with highest precedence.
     */
    public void addFirst(PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with highest search precedence",
//					propertySource.getName()));
//		}
        removeIfPresent(propertySource);
        this.propertySourceList.addFirst(propertySource);
    }

    /**
     * Add the given property source object with lowest precedence.
     */
    public void addLast(PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with lowest search precedence",
//					propertySource.getName()));
//		}
        removeIfPresent(propertySource);
        this.propertySourceList.addLast(propertySource);
    }

    /**
     * Add the given property source object with precedence immediately higher than
     * the named relative property source.
     */
    public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with search precedence immediately higher than [%s]",
//					propertySource.getName(), relativePropertySourceName));
//		}
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        removeIfPresent(propertySource);
        int index = assertPresentAndGetIndex(relativePropertySourceName);
        addAtIndex(index, propertySource);
    }

    /**
     * Add the given property source object with precedence immediately lower than
     * the named relative property source.
     */
    public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with search precedence immediately lower than [%s]",
//					propertySource.getName(), relativePropertySourceName));
//		}
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        removeIfPresent(propertySource);
        int index = assertPresentAndGetIndex(relativePropertySourceName);
        addAtIndex(index + 1, propertySource);
    }

    /**
     * Return the precedence of the given property source, {@code -1} if not found.
     */
    public int precedenceOf(PropertySource<?> propertySource) {
        return this.propertySourceList.indexOf(propertySource);
    }

    /**
     * Remove and return the property source with the given name, {@code null} if
     * not found.
     * 
     * @param name the name of the property source to find and remove
     */
    public PropertySource<?> remove(String name) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Removing [%s] PropertySource", name));
//		}
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        return index == -1 ? null : this.propertySourceList.remove(index);
    }

    /**
     * Replace the property source with the given name with the given property
     * source object.
     * 
     * @param name           the name of the property source to find and replace
     * @param propertySource the replacement property source
     * @throws IllegalArgumentException if no property source with the given name is
     *                                  present
     * @see #contains
     */
    public void replace(String name, PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Replacing [%s] PropertySource with [%s]",
//					name, propertySource.getName()));
//		}
        int index = assertPresentAndGetIndex(name);
        this.propertySourceList.set(index, propertySource);
    }

    /**
     * Return the number of {@link PropertySource} objects contained.
     */
    public int size() {
        return this.propertySourceList.size();
    }

    @Override
    public String toString() {
        String[] names = new String[this.size()];
        for (int i = 0; i < size(); i++) {
            names[i] = this.propertySourceList.get(i).getName();
        }
        return String.format("[%s]", arrayToCommaDelimitedString(names));
    }

    /**
     * Ensure that the given property source is not being added relative to itself.
     */
    protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
//		String newPropertySourceName = propertySource.getName();
//		Assert.isTrue(!relativePropertySourceName.equals(newPropertySourceName),
//				String.format(ILLEGAL_RELATIVE_ADDITION_MESSAGE, newPropertySourceName));
    }

    /**
     * Remove the given property source if it is present.
     */
    protected void removeIfPresent(PropertySource<?> propertySource) {
		this.propertySourceList.remove(propertySource);
    }

    /**
     * Add the given property source at a particular index in the list.
     */
    private void addAtIndex(int index, PropertySource<?> propertySource) {
        removeIfPresent(propertySource);
        this.propertySourceList.add(index, propertySource);
    }

    /**
     * Assert that the named property source is present and return its index.
     * 
     * @param name the {@linkplain PropertySource#getName() name of the property
     *             source} to find
     * @throws IllegalArgumentException if the named property source is not present
     */
    private int assertPresentAndGetIndex(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
//		Assert.isTrue(index >= 0, String.format(NON_EXISTENT_PROPERTY_SOURCE_MESSAGE, name));
        return index;
    }

    /**
     * Convenience method to return a String array as a delimited (e.g. CSV) String.
     * E.g. useful for {@code toString()} implementations.
     * 
     * @param arr   the array to display
     * @param delim the delimiter to use (probably a ",")
     * @return the delimited String
     */
    private static String arrayToDelimitedString(Object[] arr, String delim) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        if (arr.length == 1) {
            return nullSafeToString(arr[0]);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * Return a String representation of the specified Object.
     * <p>
     * Builds a String representation of the contents in case of an array. Returns
     * {@code "null"} if {@code obj} is {@code null}.
     * 
     * @param obj the object to build a String representation for
     * @return a String representation of {@code obj}
     */
    private static String nullSafeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Object[]) {
            return nullSafeToString((Object[]) obj);
        }
        if (obj instanceof boolean[]) {
            return nullSafeToString((boolean[]) obj);
        }
        if (obj instanceof byte[]) {
            return nullSafeToString((byte[]) obj);
        }
        if (obj instanceof char[]) {
            return nullSafeToString((char[]) obj);
        }
        if (obj instanceof double[]) {
            return nullSafeToString((double[]) obj);
        }
        if (obj instanceof float[]) {
            return nullSafeToString((float[]) obj);
        }
        if (obj instanceof int[]) {
            return nullSafeToString((int[]) obj);
        }
        if (obj instanceof long[]) {
            return nullSafeToString((long[]) obj);
        }
        if (obj instanceof short[]) {
            return nullSafeToString((short[]) obj);
        }
        String str = obj.toString();
        return (str != null ? str : "");
    }

    /**
     * Convenience method to return a String array as a CSV String. E.g. useful for
     * {@code toString()} implementations.
     * 
     * @param arr the array to display
     * @return the delimited String
     */
    private static String arrayToCommaDelimitedString(Object[] arr) {
        return arrayToDelimitedString(arr, ",");
    }
}
