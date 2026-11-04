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

import java.util.Arrays;

/**
 * Abstract base class representing a source of name/value property pairs. The
 * underlying {@linkplain #getSource() source object} may be of any type
 * {@code T} that encapsulates properties. Examples include
 * {@link java.util.Properties} objects, {@link java.util.Map} objects,
 * {@code ServletContext} and {@code ServletConfig} objects (for access to init
 * parameters). Explore the {@code PropertySource} type hierarchy to see
 * provided implementations.
 *
 * <p>
 * {@code PropertySource} objects are not typically used in isolation, but
 * rather through a {@link PropertySources} object, which aggregates property
 * sources and in conjunction with a {@link PropertyResolver} implementation
 * that can perform precedence-based searches across the set of
 * {@code PropertySources}.
 *
 * <p>
 * {@code PropertySource} identity is determined not based on the content of
 * encapsulated properties, but rather based on the {@link #getName() name} of
 * the {@code PropertySource} alone. This is useful for manipulating
 * {@code PropertySource} objects when in collection contexts. See operations in
 * {@link MutablePropertySources} as well as the {@link #named(String)} and
 * {@link #toString()} methods for details.
 *
 * <p>
 * Note that when working
 * with @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes that
 * the @{@link org.springframework.context.annotation.PropertySource
 * PropertySource} annotation provides a convenient and declarative way of
 * adding property sources to the enclosing {@code Environment}.
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> the source type
 * @see PropertySources
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see MutablePropertySources
 * @see org.springframework.context.annotation.PropertySource
 */
public abstract class PropertySource<T> {

    protected final String name;

    protected final T source;

    /**
     * Create a new {@code PropertySource} with the given name and source object.
     */
    public PropertySource(String name, T source) {
        this.name = name;
        this.source = source;
    }

    /**
     * Create a new {@code PropertySource} with the given name and with a new
     * {@code Object} instance as the underlying source.
     * <p>
     * Often useful in testing scenarios when creating anonymous implementations
     * that never query an actual source but rather return hard-coded values.
     */
    @SuppressWarnings("unchecked")
    public PropertySource(String name) {
        this(name, (T) new Object());
    }

    /**
     * Return the name of this {@code PropertySource}.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the underlying source object for this {@code PropertySource}.
     */
    public T getSource() {
        return this.source;
    }

    /**
     * Return whether this {@code PropertySource} contains the given name.
     * <p>
     * This implementation simply checks for a {@code null} return value from
     * {@link #getProperty(String)}. Subclasses may wish to implement a more
     * efficient algorithm if possible.
     * 
     * @param name the property name to find
     */
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * Return the value associated with the given name, or {@code null} if not
     * found.
     * 
     * @param name the property to find
     * @see PropertyResolver#getRequiredProperty(String)
     */
    public abstract Object getProperty(String name);

    /**
     * This {@code PropertySource} object is equal to the given object if:
     * <ul>
     * <li>they are the same instance
     * <li>the {@code name} properties for both objects are equal
     * </ul>
     * <p>
     * No properties other than {@code name} are evaluated.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other
                || (other instanceof PropertySource && nullSafeEquals(this.name, ((PropertySource<?>) other).name)));
    }

    /**
     * Return a hash code derived from the {@code name} property of this
     * {@code PropertySource} object.
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * Produce concise output (type and name) if the current log level does not
     * include debug. If debug is enabled, produce verbose output including the hash
     * code of the PropertySource instance and every name/value property pair.
     * <p>
     * This variable verbosity is useful as a property source such as system
     * properties or environment variables may contain an arbitrary number of
     * property pairs, potentially leading to difficult to read exception and log
     * messages.
     * 
     * @see Log#isDebugEnabled()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name='" + this.name + "'}";
    }

    /**
     * Return a {@code PropertySource} implementation intended for collection
     * comparison purposes only.
     * <p>
     * Primarily for internal use, but given a collection of {@code PropertySource}
     * objects, may be used as follows:
     * 
     * <pre class="code">
     * {
     *     &#64;code
     *     List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
     *     sources.add(new MapPropertySource("sourceA", mapA));
     *     sources.add(new MapPropertySource("sourceB", mapB));
     *     assert sources.contains(PropertySource.named("sourceA"));
     *     assert sources.contains(PropertySource.named("sourceB"));
     *     assert !sources.contains(PropertySource.named("sourceC"));
     * }
     * </pre>
     * 
     * The returned {@code PropertySource} will throw
     * {@code UnsupportedOperationException} if any methods other than
     * {@code equals(Object)}, {@code hashCode()}, and {@code toString()} are
     * called.
     * 
     * @param name the name of the comparison {@code PropertySource} to be created
     *             and returned.
     */
    public static PropertySource<?> named(String name) {
        return new ComparisonPropertySource(name);
    }

    /**
     * Determine if the given objects are equal, returning {@code true} if both are
     * {@code null} or {@code false} if only one is {@code null}.
     * <p>
     * Compares arrays with {@code Arrays.equals}, performing an equality check
     * based on the array elements rather than the array reference.
     * 
     * @param o1 first Object to compare
     * @param o2 second Object to compare
     * @return whether the given objects are equal
     * @see Object#equals(Object)
     * @see java.util.Arrays#equals
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals(o1, o2);
        }
        return false;
    }

    /**
     * Compare the given arrays with {@code Arrays.equals}, performing an equality
     * check based on the array elements rather than the array reference.
     * 
     * @param o1 first array to compare
     * @param o2 second array to compare
     * @return whether the given objects are equal
     * @see #nullSafeEquals(Object, Object)
     * @see java.util.Arrays#equals
     */
    private static boolean arrayEquals(Object o1, Object o2) {
        if (o1 instanceof Object[] && o2 instanceof Object[]) {
            return Arrays.equals((Object[]) o1, (Object[]) o2);
        }
        if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
            return Arrays.equals((boolean[]) o1, (boolean[]) o2);
        }
        if (o1 instanceof byte[] && o2 instanceof byte[]) {
            return Arrays.equals((byte[]) o1, (byte[]) o2);
        }
        if (o1 instanceof char[] && o2 instanceof char[]) {
            return Arrays.equals((char[]) o1, (char[]) o2);
        }
        if (o1 instanceof double[] && o2 instanceof double[]) {
            return Arrays.equals((double[]) o1, (double[]) o2);
        }
        if (o1 instanceof float[] && o2 instanceof float[]) {
            return Arrays.equals((float[]) o1, (float[]) o2);
        }
        if (o1 instanceof int[] && o2 instanceof int[]) {
            return Arrays.equals((int[]) o1, (int[]) o2);
        }
        if (o1 instanceof long[] && o2 instanceof long[]) {
            return Arrays.equals((long[]) o1, (long[]) o2);
        }
        if (o1 instanceof short[] && o2 instanceof short[]) {
            return Arrays.equals((short[]) o1, (short[]) o2);
        }
        return false;
    }

    /**
     * {@code PropertySource} to be used as a placeholder in cases where an actual
     * property source cannot be eagerly initialized at application context creation
     * time. For example, a {@code ServletContext}-based property source must wait
     * until the {@code ServletContext} object is available to its enclosing
     * {@code ApplicationContext}. In such cases, a stub should be used to hold the
     * intended default position/order of the property source, then be replaced
     * during context refresh.
     * 
     * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
     * @see org.springframework.web.context.support.StandardServletEnvironment
     * @see org.springframework.web.context.support.ServletContextPropertySource
     */
    public static class StubPropertySource extends PropertySource<Object> {

        public StubPropertySource(String name) {
            super(name, new Object());
        }

        /**
         * Always returns {@code null}.
         */
        @Override
        public String getProperty(String name) {
            return null;
        }
    }

    /**
     * @see PropertySource#named(String)
     */
    static class ComparisonPropertySource extends StubPropertySource {

        private static final String USAGE_ERROR = "ComparisonPropertySource instances are for use with collection comparison only";

        public ComparisonPropertySource(String name) {
            super(name);
        }

        @Override
        public Object getSource() {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public boolean containsProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public String getProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public String toString() {
            return String.format("%s [name='%s']", getClass().getSimpleName(), this.name);
        }
    }
}
