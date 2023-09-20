/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.arthas.grpcweb.proxy.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * <p>A pair consisting of two elements.</p>
 *
 * <p>This class is an abstract implementation defining the basic API.
 * It refers to the elements as 'left' and 'right'. It also implements the
 * {@code Map.Entry} interface where the key is 'left' and the value is 'right'.</p>
 *
 * <p>Subclass implementations may be mutable or immutable.
 * However, there is no restriction on the type of the stored objects that may be stored.
 * If mutable objects are stored in the pair, then the pair itself effectively becomes mutable.</p>
 *
 * @param <L> the left element type
 * @param <R> the right element type
 *
 * @since 3.0
 */
public abstract class Pair<L, R> implements Map.Entry<L, R>,  Serializable {

    private static final class PairAdapter<L, R> extends Pair<L, R> {

        private static final long serialVersionUID = 1L;

        @Override
        public L getLeft() {
            return null;
        }

        @Override
        public R getRight() {
            return null;
        }

        @Override
        public R setValue(final R value) {
            return null;
        }

    }

    /** Serialization version */
    private static final long serialVersionUID = 4954918890077093841L;

    /**
     * An empty array.
     * <p>
     * Consider using {@link #emptyArray()} to avoid generics warnings.
     * </p>
     *
     * @since 3.10.
     */
    public static final Pair<?, ?>[] EMPTY_ARRAY = new PairAdapter[0];

    /**
     * Returns the empty array singleton that can be assigned without compiler warning.
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @return the empty array singleton that can be assigned without compiler warning.
     *
     * @since 3.10.
     */
    @SuppressWarnings("unchecked")
    public static <L, R> Pair<L, R>[] emptyArray() {
        return (Pair<L, R>[]) EMPTY_ARRAY;
    }

    /**
     * <p>Creates an immutable pair of two objects inferring the generic types.</p>
     *
     * <p>This factory allows the pair to be created using inference to
     * obtain the generic types.</p>
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @param left  the left element, may be null
     * @param right  the right element, may be null
     * @return a pair formed from the two parameters, not null
     */
    public static <L, R> Pair<L, R> of(final L left, final R right) {
        return ImmutablePair.of(left, right);
    }

    /**
     * <p>Creates an immutable pair from an existing pair.</p>
     *
     * <p>This factory allows the pair to be created using inference to
     * obtain the generic types.</p>
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @param pair the existing pair.
     * @return a pair formed from the two parameters, not null
     * @since 3.10
     */
    public static <L, R> Pair<L, R> of(final Map.Entry<L, R> pair) {
        return ImmutablePair.of(pair);
    }


    /**
     * <p>Compares this pair to another based on the two elements.</p>
     *
     * @param obj  the object to compare to, null returns false
     * @return true if the elements of the pair are equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Map.Entry<?, ?>) {
            final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
            return Objects.equals(getKey(), other.getKey())
                    && Objects.equals(getValue(), other.getValue());
        }
        return false;
    }

    /**
     * <p>Gets the key from this pair.</p>
     *
     * <p>This method implements the {@code Map.Entry} interface returning the
     * left element as the key.</p>
     *
     * @return the left element as the key, may be null
     */
    @Override
    public final L getKey() {
        return getLeft();
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Gets the left element from this pair.</p>
     *
     * <p>When treated as a key-value pair, this is the key.</p>
     *
     * @return the left element, may be null
     */
    public abstract L getLeft();

    /**
     * <p>Gets the right element from this pair.</p>
     *
     * <p>When treated as a key-value pair, this is the value.</p>
     *
     * @return the right element, may be null
     */
    public abstract R getRight();

    /**
     * <p>Gets the value from this pair.</p>
     *
     * <p>This method implements the {@code Map.Entry} interface returning the
     * right element as the value.</p>
     *
     * @return the right element as the value, may be null
     */
    @Override
    public R getValue() {
        return getRight();
    }

    /**
     * <p>Returns a suitable hash code.
     * The hash code follows the definition in {@code Map.Entry}.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        // see Map.Entry API specification
        return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
    }

    /**
     * <p>Returns a String representation of this pair using the format {@code ($left,$right)}.</p>
     *
     * @return a string describing this object, not null
     */
    @Override
    public String toString() {
        return "(" + getLeft() + ',' + getRight() + ')';
    }

    /**
     * <p>Formats the receiver using the given format.</p>
     *
     * <p>This uses {@link java.util.Formattable} to perform the formatting. Two variables may
     * be used to embed the left and right elements. Use {@code %1$s} for the left
     * element (key) and {@code %2$s} for the right element (value).
     * The default format used by {@code toString()} is {@code (%1$s,%2$s)}.</p>
     *
     * @param format  the format string, optionally containing {@code %1$s} and {@code %2$s}, not null
     * @return the formatted string, not null
     */
    public String toString(final String format) {
        return String.format(format, getLeft(), getRight());
    }

}
