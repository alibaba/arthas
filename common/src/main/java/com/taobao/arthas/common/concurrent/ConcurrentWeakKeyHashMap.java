/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */
package com.taobao.arthas.common.concurrent;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;


/**
 * An alternative weak-key {@link ConcurrentMap} which is similar to
 * {@link ConcurrentHashMap}.
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public final class ConcurrentWeakKeyHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    /*
     * The basic strategy is to subdivide the table among Segments,
     * each of which itself is a concurrently readable hash table.
     */

    /**
     * The default initial capacity for this table, used when not otherwise
     * specified in a constructor.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The default load factor for this table, used when not otherwise specified
     * in a constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The default concurrency level for this table, used when not otherwise
     * specified in a constructor.
     */
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified by
     * either of the constructors with arguments.  MUST be a power of two
     * &lt;= 1&lt;&lt;30 to ensure that entries are indexable using integers.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The maximum number of segments to allow; used to bound constructor
     * arguments.
     */
    static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

    /**
     * Number of unsynchronized retries in size and containsValue methods before
     * resorting to locking. This is used to avoid unbounded retries if tables
     * undergo continuous modification which would make it impossible to obtain
     * an accurate result.
     */
    static final int RETRIES_BEFORE_LOCK = 2;

    /* ---------------- Fields -------------- */

    /**
     * Mask value for indexing into segments. The upper bits of a key's hash
     * code are used to choose the segment.
     */
    final int segmentMask;

    /**
     * Shift value for indexing within segments.
     */
    final int segmentShift;

    /**
     * The segments, each of which is a specialized hash table
     */
    final Segment<K, V>[] segments;

    Set<K> keySet;
    Set<Map.Entry<K, V>> entrySet;
    Collection<V> values;

    /* ---------------- Small Utilities -------------- */

    /**
     * Applies a supplemental hash function to a given hashCode, which defends
     * against poor quality hash functions.  This is critical because
     * ConcurrentReferenceHashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ in lower
     * or upper bits.
     */
    private static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += h << 15 ^ 0xffffcd7d;
        h ^= h >>> 10;
        h += h << 3;
        h ^= h >>> 6;
        h += (h << 2) + (h << 14);
        return h ^ h >>> 16;
    }

    /**
     * Returns the segment that should be used for key with given hash.
     *
     * @param hash the hash code for the key
     * @return the segment
     */
    Segment<K, V> segmentFor(int hash) {
        return segments[hash >>> segmentShift & segmentMask];
    }

    private static int hashOf(Object key) {
        return hash(key.hashCode());
    }

    /* ---------------- Inner Classes -------------- */

    /**
     * A weak-key reference which stores the key hash needed for reclamation.
     */
    static final class WeakKeyReference<K> extends WeakReference<K> {

        final int hash;

        WeakKeyReference(K key, int hash, ReferenceQueue<Object> refQueue) {
            super(key, refQueue);
            this.hash = hash;
        }

        public int keyHash() {
            return hash;
        }

        public Object keyRef() {
            return this;
        }
    }

    /**
     * ConcurrentReferenceHashMap list entry. Note that this is never exported
     * out as a user-visible Map.Entry.
     *
     * Because the value field is volatile, not final, it is legal wrt
     * the Java Memory Model for an unsynchronized reader to see null
     * instead of initial value when read via a data race.  Although a
     * reordering leading to this is not likely to ever actually
     * occur, the Segment.readValueUnderLock method is used as a
     * backup in case a null (pre-initialized) value is ever seen in
     * an unsynchronized access method.
     */
    static final class HashEntry<K, V> {
        final Object keyRef;
        final int hash;
        volatile Object valueRef;
        final HashEntry<K, V> next;

        HashEntry(
                K key, int hash, HashEntry<K, V> next, V value,
                ReferenceQueue<Object> refQueue) {
            this.hash = hash;
            this.next = next;
            keyRef = new WeakKeyReference<K>(key, hash, refQueue);
            valueRef = value;
        }

        @SuppressWarnings("unchecked")
        K key() {
            return ((Reference<K>) keyRef).get();
        }

        V value() {
            return dereferenceValue(valueRef);
        }

        @SuppressWarnings("unchecked")
        V dereferenceValue(Object value) {
            if (value instanceof WeakKeyReference) {
                return ((Reference<V>) value).get();
            }

            return (V) value;
        }

        void setValue(V value) {
            valueRef = value;
        }

        @SuppressWarnings("unchecked")
        static <K, V> HashEntry<K, V>[] newArray(int i) {
            return new HashEntry[i];
        }
    }

    /**
     * Segments are specialized versions of hash tables.  This subclasses from
     * ReentrantLock opportunistically, just to simplify some locking and avoid
     * separate construction.
     */
    static final class Segment<K, V> extends ReentrantLock {
        /*
         * Segments maintain a table of entry lists that are ALWAYS kept in a
         * consistent state, so can be read without locking. Next fields of
         * nodes are immutable (final).  All list additions are performed at the
         * front of each bin. This makes it easy to check changes, and also fast
         * to traverse. When nodes would otherwise be changed, new nodes are
         * created to replace them. This works well for hash tables since the
         * bin lists tend to be short. (The average length is less than two for
         * the default load factor threshold.)
         *
         * Read operations can thus proceed without locking, but rely on
         * selected uses of volatiles to ensure that completed write operations
         * performed by other threads are noticed. For most purposes, the
         * "count" field, tracking the number of elements, serves as that
         * volatile variable ensuring visibility.  This is convenient because
         * this field needs to be read in many read operations anyway:
         *
         *   - All (unsynchronized) read operations must first read the
         *     "count" field, and should not look at table entries if
         *     it is 0.
         *
         *   - All (synchronized) write operations should write to
         *     the "count" field after structurally changing any bin.
         *     The operations must not take any action that could even
         *     momentarily cause a concurrent read operation to see
         *     inconsistent data. This is made easier by the nature of
         *     the read operations in Map. For example, no operation
         *     can reveal that the table has grown but the threshold
         *     has not yet been updated, so there are no atomicity
         *     requirements for this with respect to reads.
         *
         * As a guide, all critical volatile reads and writes to the count field
         * are marked in code comments.
         */

        private static final long serialVersionUID = -8328104880676891126L;

        /**
         * The number of elements in this segment's region.
         */
        transient volatile int count;

        /**
         * Number of updates that alter the size of the table. This is used
         * during bulk-read methods to make sure they see a consistent snapshot:
         * If modCounts change during a traversal of segments computing size or
         * checking containsValue, then we might have an inconsistent view of
         * state so (usually) must retry.
         */
        int modCount;

        /**
         * The table is rehashed when its size exceeds this threshold.
         * (The value of this field is always <tt>(capacity * loadFactor)</tt>.)
         */
        int threshold;

        /**
         * The per-segment table.
         */
        transient volatile HashEntry<K, V>[] table;

        /**
         * The load factor for the hash table.  Even though this value is same
         * for all segments, it is replicated to avoid needing links to outer
         * object.
         */
        final float loadFactor;

        /**
         * The collected weak-key reference queue for this segment. This should
         * be (re)initialized whenever table is assigned,
         */
        transient volatile ReferenceQueue<Object> refQueue;

        Segment(int initialCapacity, float lf) {
            loadFactor = lf;
            setTable(HashEntry.<K, V>newArray(initialCapacity));
        }

        @SuppressWarnings("unchecked")
        static <K, V> Segment<K, V>[] newArray(int i) {
            return new Segment[i];
        }

        private static boolean keyEq(Object src, Object dest) {
            return src.equals(dest);
        }

        /**
         * Sets table to new HashEntry array. Call only while holding lock or in
         * constructor.
         */
        void setTable(HashEntry<K, V>[] newTable) {
            threshold = (int) (newTable.length * loadFactor);
            table = newTable;
            refQueue = new ReferenceQueue<Object>();
        }

        /**
         * Returns properly casted first entry of bin for given hash.
         */
        HashEntry<K, V> getFirst(int hash) {
            HashEntry<K, V>[] tab = table;
            return tab[hash & tab.length - 1];
        }

        HashEntry<K, V> newHashEntry(
                K key, int hash, HashEntry<K, V> next, V value) {
            return new HashEntry<K, V>(
                    key, hash, next, value, refQueue);
        }

        /**
         * Reads value field of an entry under lock. Called if value field ever
         * appears to be null. This is possible only if a compiler happens to
         * reorder a HashEntry initialization with its table assignment, which
         * is legal under memory model but is not known to ever occur.
         */
        V readValueUnderLock(HashEntry<K, V> e) {
            lock();
            try {
                removeStale();
                return e.value();
            } finally {
                unlock();
            }
        }

        /* Specialized implementations of map methods */

        V get(Object key, int hash) {
            if (count != 0) { // read-volatile
                HashEntry<K, V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && keyEq(key, e.key())) {
                        Object opaque = e.valueRef;
                        if (opaque != null) {
                            return e.dereferenceValue(opaque);
                        }

                        return readValueUnderLock(e); // recheck
                    }
                    e = e.next;
                }
            }
            return null;
        }

        boolean containsKey(Object key, int hash) {
            if (count != 0) { // read-volatile
                HashEntry<K, V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && keyEq(key, e.key())) {
                        return true;
                    }
                    e = e.next;
                }
            }
            return false;
        }

        boolean containsValue(Object value) {
            if (count != 0) { // read-volatile
                for (HashEntry<K, V> e: table) {
                    for (; e != null; e = e.next) {
                        Object opaque = e.valueRef;
                        V v;

                        if (opaque == null) {
                            v = readValueUnderLock(e); // recheck
                        } else {
                            v = e.dereferenceValue(opaque);
                        }

                        if (value.equals(v)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        boolean replace(K key, int hash, V oldValue, V newValue) {
            lock();
            try {
                removeStale();
                HashEntry<K, V> e = getFirst(hash);
                while (e != null && (e.hash != hash || !keyEq(key, e.key()))) {
                    e = e.next;
                }

                boolean replaced = false;
                if (e != null && oldValue.equals(e.value())) {
                    replaced = true;
                    e.setValue(newValue);
                }
                return replaced;
            } finally {
                unlock();
            }
        }

        V replace(K key, int hash, V newValue) {
            lock();
            try {
                removeStale();
                HashEntry<K, V> e = getFirst(hash);
                while (e != null && (e.hash != hash || !keyEq(key, e.key()))) {
                    e = e.next;
                }

                V oldValue = null;
                if (e != null) {
                    oldValue = e.value();
                    e.setValue(newValue);
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        V put(K key, int hash, V value, boolean onlyIfAbsent) {
            lock();
            try {
                removeStale();
                int c = count;
                if (c ++ > threshold) { // ensure capacity
                    int reduced = rehash();
                    if (reduced > 0) {
                        count = (c -= reduced) - 1; // write-volatile
                    }
                }

                HashEntry<K, V>[] tab = table;
                int index = hash & tab.length - 1;
                HashEntry<K, V> first = tab[index];
                HashEntry<K, V> e = first;
                while (e != null && (e.hash != hash || !keyEq(key, e.key()))) {
                    e = e.next;
                }

                V oldValue;
                if (e != null) {
                    oldValue = e.value();
                    if (!onlyIfAbsent) {
                        e.setValue(value);
                    }
                } else {
                    oldValue = null;
                    ++ modCount;
                    tab[index] = newHashEntry(key, hash, first, value);
                    count = c; // write-volatile
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        int rehash() {
            HashEntry<K, V>[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                return 0;
            }

            /*
             * Reclassify nodes in each list to new Map.  Because we are using
             * power-of-two expansion, the elements from each bin must either
             * stay at same index, or move with a power of two offset. We
             * eliminate unnecessary node creation by catching cases where old
             * nodes can be reused because their next fields won't change.
             * Statistically, at the default threshold, only about one-sixth of
             * them need cloning when a table doubles. The nodes they replace
             * will be garbage collectable as soon as they are no longer
             * referenced by any reader thread that may be in the midst of
             * traversing table right now.
             */

            HashEntry<K, V>[] newTable = HashEntry.newArray(oldCapacity << 1);
            threshold = (int) (newTable.length * loadFactor);
            int sizeMask = newTable.length - 1;
            int reduce = 0;
            for (HashEntry<K, V> e: oldTable) {
                // We need to guarantee that any existing reads of old Map can
                // proceed. So we cannot yet null out each bin.
                if (e != null) {
                    HashEntry<K, V> next = e.next;
                    int idx = e.hash & sizeMask;

                    // Single node on list
                    if (next == null) {
                        newTable[idx] = e;
                    } else {
                        // Reuse trailing consecutive sequence at same slot
                        HashEntry<K, V> lastRun = e;
                        int lastIdx = idx;
                        for (HashEntry<K, V> last = next; last != null; last = last.next) {
                            int k = last.hash & sizeMask;
                            if (k != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }
                        newTable[lastIdx] = lastRun;
                        // Clone all remaining nodes
                        for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {
                            // Skip GC'd weak references
                            K key = p.key();
                            if (key == null) {
                                reduce++;
                                continue;
                            }
                            int k = p.hash & sizeMask;
                            HashEntry<K, V> n = newTable[k];
                            newTable[k] = newHashEntry(key, p.hash, n, p.value());
                        }
                    }
                }
            }
            table = newTable;
            return reduce;
        }

        /**
         * Remove; match on key only if value null, else match both.
         */
        V remove(Object key, int hash, Object value, boolean refRemove) {
            lock();
            try {
                if (!refRemove) {
                    removeStale();
                }
                int c = count - 1;
                HashEntry<K, V>[] tab = table;
                int index = hash & tab.length - 1;
                HashEntry<K, V> first = tab[index];
                HashEntry<K, V> e = first;
                // a reference remove operation compares the Reference instance
                while (e != null && key != e.keyRef &&
                        (refRemove || hash != e.hash || !keyEq(key, e.key()))) {
                    e = e.next;
                }

                V oldValue = null;
                if (e != null) {
                    V v = e.value();
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        // All entries following removed node can stay in list,
                        // but all preceding ones need to be cloned.
                        ++ modCount;
                        HashEntry<K, V> newFirst = e.next;
                        for (HashEntry<K, V> p = first; p != e; p = p.next) {
                            K pKey = p.key();
                            if (pKey == null) { // Skip GC'd keys
                                c --;
                                continue;
                            }

                            newFirst = newHashEntry(
                                    pKey, p.hash, newFirst, p.value());
                        }
                        tab[index] = newFirst;
                        count = c; // write-volatile
                    }
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        @SuppressWarnings("rawtypes")
        void removeStale() {
            WeakKeyReference ref;
            while ((ref = (WeakKeyReference) refQueue.poll()) != null) {
                remove(ref.keyRef(), ref.keyHash(), null, true);
            }
        }

        void clear() {
            if (count != 0) {
                lock();
                try {
                    HashEntry<K, V>[] tab = table;
                    for (int i = 0; i < tab.length; i ++) {
                        tab[i] = null;
                    }
                    ++ modCount;
                    // replace the reference queue to avoid unnecessary stale
                    // cleanups
                    refQueue = new ReferenceQueue<Object>();
                    count = 0; // write-volatile
                } finally {
                    unlock();
                }
            }
        }
    }

    /* ---------------- Public operations -------------- */

    /**
     * Creates a new, empty map with the specified initial capacity, load factor
     * and concurrency level.
     *
     * @param initialCapacity the initial capacity. The implementation performs
     *                        internal sizing to accommodate this many elements.
     * @param loadFactor the load factor threshold, used to control resizing.
     *                   Resizing may be performed when the average number of
     *                   elements per bin exceeds this threshold.
     * @param concurrencyLevel the estimated number of concurrently updating
     *                         threads. The implementation performs internal
     *                         sizing to try to accommodate this many threads.
     * @throws IllegalArgumentException if the initial capacity is negative or
     *                                  the load factor or concurrencyLevel are
     *                                  nonpositive.
     */
    public ConcurrentWeakKeyHashMap(
            int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }

        if (concurrencyLevel > MAX_SEGMENTS) {
            concurrencyLevel = MAX_SEGMENTS;
        }

        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++ sshift;
            ssize <<= 1;
        }
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        segments = Segment.newArray(ssize);

        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity) {
            ++ c;
        }
        int cap = 1;
        while (cap < c) {
            cap <<= 1;
        }

        for (int i = 0; i < segments.length; ++ i) {
            segments[i] = new Segment<K, V>(cap, loadFactor);
        }
    }

    /**
     * Creates a new, empty map with the specified initial capacity and load
     * factor and with the default reference types (weak keys, strong values),
     * and concurrencyLevel (16).
     *
     * @param initialCapacity The implementation performs internal sizing to
     *                        accommodate this many elements.
     * @param loadFactor the load factor threshold, used to control resizing.
     *                   Resizing may be performed when the average number of
     *                   elements per bin exceeds this threshold.
     * @throws IllegalArgumentException if the initial capacity of elements is
     *                                  negative or the load factor is
     *                                  nonpositive
     */
    public ConcurrentWeakKeyHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Creates a new, empty map with the specified initial capacity, and with
     * default reference types (weak keys, strong values), load factor (0.75)
     * and concurrencyLevel (16).
     *
     * @param initialCapacity the initial capacity. The implementation performs
     *                        internal sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of elements is
     *                                  negative.
     */
    public ConcurrentWeakKeyHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Creates a new, empty map with a default initial capacity (16), reference
     * types (weak keys, strong values), default load factor (0.75) and
     * concurrencyLevel (16).
     */
    public ConcurrentWeakKeyHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Creates a new map with the same mappings as the given map. The map is
     * created with a capacity of 1.5 times the number of mappings in the given
     * map or 16 (whichever is greater), and a default load factor (0.75) and
     * concurrencyLevel (16).
     *
     * @param m the map
     */
    public ConcurrentWeakKeyHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
             DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR,
             DEFAULT_CONCURRENCY_LEVEL);
        putAll(m);
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        final Segment<K, V>[] segments = this.segments;
        /*
         * We keep track of per-segment modCounts to avoid ABA problems in which
         * an element in one segment was added and in another removed during
         * traversal, in which case the table was never actually empty at any
         * point. Note the similar use of modCounts in the size() and
         * containsValue() methods, which are the only other methods also
         * susceptible to ABA problems.
         */
        int[] mc = new int[segments.length];
        int mcsum = 0;
        for (int i = 0; i < segments.length; ++ i) {
            if (segments[i].count != 0) {
                return false;
            } else {
                mcsum += mc[i] = segments[i].modCount;
            }
        }
        // If mcsum happens to be zero, then we know we got a snapshot before
        // any modifications at all were made.  This is probably common enough
        // to bother tracking.
        if (mcsum != 0) {
            for (int i = 0; i < segments.length; ++ i) {
                if (segments[i].count != 0 || mc[i] != segments[i].modCount) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the number of key-value mappings in this map. If the map contains
     * more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        final Segment<K, V>[] segments = this.segments;
        long sum = 0;
        long check = 0;
        int[] mc = new int[segments.length];
        // Try a few times to get accurate count. On failure due to continuous
        // async changes in table, resort to locking.
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++ k) {
            check = 0;
            sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++ i) {
                sum += segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++ i) {
                    check += segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        check = -1; // force retry
                        break;
                    }
                }
            }
            if (check == sum) {
                break;
            }
        }
        if (check != sum) { // Resort to locking all segments
            sum = 0;
            for (Segment<K, V> segment: segments) {
                segment.lock();
            }
            for (Segment<K, V> segment: segments) {
                sum += segment.count;
            }
            for (Segment<K, V> segment: segments) {
                segment.unlock();
            }
        }
        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) sum;
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key {@code k} to
     * a value {@code v} such that {@code key.equals(k)}, then this method
     * returns {@code v}; otherwise it returns {@code null}.  (There can be at
     * most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public V get(Object key) {
        int hash = hashOf(key);
        return segmentFor(hash).get(key, hash);
    }

    /**
     * Tests if the specified object is a key in this table.
     *
     * @param  key   possible key
     * @return <tt>true</tt> if and only if the specified object is a key in
     *         this table, as determined by the <tt>equals</tt> method;
     *         <tt>false</tt> otherwise.
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public boolean containsKey(Object key) {
        int hash = hashOf(key);
        return segmentFor(hash).containsKey(key, hash);
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified
     * value. Note: This method requires a full internal traversal of the hash
     * table, and so is much slower than method <tt>containsKey</tt>.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the specified
     *         value
     * @throws NullPointerException if the specified value is null
     */

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        // See explanation of modCount use above

        final Segment<K, V>[] segments = this.segments;
        int[] mc = new int[segments.length];

        // Try a few times without locking
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++ k) {
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++ i) {
                mcsum += mc[i] = segments[i].modCount;
                if (segments[i].containsValue(value)) {
                    return true;
                }
            }
            boolean cleanSweep = true;
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++ i) {
                    if (mc[i] != segments[i].modCount) {
                        cleanSweep = false;
                        break;
                    }
                }
            }
            if (cleanSweep) {
                return false;
            }
        }
        // Resort to locking all segments
        for (Segment<K, V> segment: segments) {
            segment.lock();
        }
        boolean found = false;
        try {
            for (Segment<K, V> segment: segments) {
                if (segment.containsValue(value)) {
                    found = true;
                    break;
                }
            }
        } finally {
            for (Segment<K, V> segment: segments) {
                segment.unlock();
            }
        }
        return found;
    }

    /**
     * Legacy method testing if some key maps into the specified value in this
     * table.  This method is identical in functionality to
     * {@link #containsValue}, and exists solely to ensure full compatibility
     * with class {@link Hashtable}, which supported this method prior to
     * introduction of the Java Collections framework.
     *
     * @param  value a value to search for
     * @return <tt>true</tt> if and only if some key maps to the <tt>value</tt>
     *         argument in this table as determined by the <tt>equals</tt>
     *         method; <tt>false</tt> otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * Maps the specified key to the specified value in this table.  Neither the
     * key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the <tt>get</tt> method with a
     * key that is equal to the original key.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     *         if there was no mapping for <tt>key</tt>
     * @throws NullPointerException if the specified key or value is null
     */
    @Override
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        int hash = hashOf(key);
        return segmentFor(hash).put(key, hash, value, false);
    }

    /**
     * @return the previous value associated with the specified key, or
     *         <tt>null</tt> if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V putIfAbsent(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        int hash = hashOf(key);
        return segmentFor(hash).put(key, hash, value, true);
    }

    /**
     * Copies all of the mappings from the specified map to this one.  These
     * mappings replace any mappings that this map had for any of the keys
     * currently in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e: m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes the key (and its corresponding value) from this map.  This method
     * does nothing if the key is not in the map.
     *
     * @param  key the key that needs to be removed
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     *         if there was no mapping for <tt>key</tt>
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public V remove(Object key) {
        int hash = hashOf(key);
        return segmentFor(hash).remove(key, hash, null, false);
    }

    /**
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(Object key, Object value) {
        int hash = hashOf(key);
        if (value == null) {
            return false;
        }
        return segmentFor(hash).remove(key, hash, value, false) != null;
    }

    /**
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        int hash = hashOf(key);
        return segmentFor(hash).replace(key, hash, oldValue, newValue);
    }

    /**
     * @return the previous value associated with the specified key, or
     *         <tt>null</tt> if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        int hash = hashOf(key);
        return segmentFor(hash).replace(key, hash, value);
    }

    /**
     * Removes all of the mappings from this map.
     */
    @Override
    public void clear() {
        for (Segment<K, V> segment: segments) {
            segment.clear();
        }
    }

    /**
     * Removes any stale entries whose keys have been finalized. Use of this
     * method is normally not necessary since stale entries are automatically
     * removed lazily, when blocking operations are required. However, there are
     * some cases where this operation should be performed eagerly, such as
     * cleaning up old references to a ClassLoader in a multi-classloader
     * environment.
     *
     * Note: this method will acquire locks, one at a time, across all segments
     * of this table, so if it is to be used, it should be used sparingly.
     */
    public void purgeStaleEntries() {
        for (Segment<K, V> segment: segments) {
            segment.removeStale();
        }
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator that
     * will never throw {@link ConcurrentModificationException}, and guarantees
     * to traverse elements as they existed upon construction of the iterator,
     * and may (but is not guaranteed to) reflect any modifications subsequent
     * to construction.
     */
    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return ks != null? ks : (keySet = new KeySet());
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are reflected
     * in the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt>, and <tt>clear</tt> operations.  It does not support
     * the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator that
     * will never throw {@link ConcurrentModificationException}, and guarantees
     * to traverse elements as they existed upon construction of the iterator,
     * and may (but is not guaranteed to) reflect any modifications subsequent
     * to construction.
     */
    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        return vs != null? vs : (values = new Values());
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are reflected in the
     * set, and vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator that
     * will never throw {@link ConcurrentModificationException}, and guarantees
     * to traverse elements as they existed upon construction of the iterator,
     * and may (but is not guaranteed to) reflect any modifications subsequent
     * to construction.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es = entrySet;
        return es != null? es : (entrySet = new EntrySet());
    }

    /**
     * Returns an enumeration of the keys in this table.
     *
     * @return an enumeration of the keys in this table
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        return new KeyIterator();
    }

    /**
     * Returns an enumeration of the values in this table.
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    public Enumeration<V> elements() {
        return new ValueIterator();
    }

    /* ---------------- Iterator Support -------------- */

    abstract class HashIterator {
        int nextSegmentIndex;
        int nextTableIndex;
        HashEntry<K, V>[] currentTable;
        HashEntry<K, V> nextEntry;
        HashEntry<K, V> lastReturned;
        K currentKey; // Strong reference to weak key (prevents gc)

        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        public void rewind() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            currentTable = null;
            nextEntry = null;
            lastReturned = null;
            currentKey = null;
            advance();
        }

        public boolean hasMoreElements() {
            return hasNext();
        }

        final void advance() {
            if (nextEntry != null && (nextEntry = nextEntry.next) != null) {
                return;
            }

            while (nextTableIndex >= 0) {
                if ((nextEntry = currentTable[nextTableIndex --]) != null) {
                    return;
                }
            }

            while (nextSegmentIndex >= 0) {
                Segment<K, V> seg = segments[nextSegmentIndex --];
                if (seg.count != 0) {
                    currentTable = seg.table;
                    for (int j = currentTable.length - 1; j >= 0; -- j) {
                        if ((nextEntry = currentTable[j]) != null) {
                            nextTableIndex = j - 1;
                            return;
                        }
                    }
                }
            }
        }

        public boolean hasNext() {
            while (nextEntry != null) {
                if (nextEntry.key() != null) {
                    return true;
                }
                advance();
            }

            return false;
        }

        HashEntry<K, V> nextEntry() {
            do {
                if (nextEntry == null) {
                    throw new NoSuchElementException();
                }

                lastReturned = nextEntry;
                currentKey = lastReturned.key();
                advance();
            } while (currentKey == null); // Skip GC'd keys

            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            ConcurrentWeakKeyHashMap.this.remove(currentKey);
            lastReturned = null;
        }
    }

    final class KeyIterator
            extends HashIterator implements ReusableIterator<K>, Enumeration<K> {

        public K next() {
            return nextEntry().key();
        }

        public K nextElement() {
            return nextEntry().key();
        }
    }

    final class ValueIterator
            extends HashIterator implements ReusableIterator<V>, Enumeration<V> {

        public V next() {
            return nextEntry().value();
        }

        public V nextElement() {
            return nextEntry().value();
        }
    }

    /*
     * This class is needed for JDK5 compatibility.
     */
    static class SimpleEntry<K, V> implements Entry<K, V> {

        private final K key;

        private V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(Entry<? extends K, ? extends V> entry) {
            key = entry.getKey();
            value = entry.getValue();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?>)) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            Map.Entry e = (Map.Entry) o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return (key == null? 0 : key.hashCode()) ^ (value == null? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

        private static boolean eq(Object o1, Object o2) {
            return o1 == null? o2 == null : o1.equals(o2);
        }
    }

    /**
     * Custom Entry class used by EntryIterator.next(), that relays setValue
     * changes to the underlying map.
     */
    final class WriteThroughEntry extends SimpleEntry<K, V> {

        WriteThroughEntry(K k, V v) {
            super(k, v);
        }

        /**
         * Set our entry's value and write through to the map. The value to
         * return is somewhat arbitrary here. Since a WriteThroughEntry does not
         * necessarily track asynchronous changes, the most recent "previous"
         * value could be different from what we return (or could even have been
         * removed in which case the put will re-establish). We do not and can
         * not guarantee more.
         */
        @Override
        public V setValue(V value) {

            if (value == null) {
                throw new NullPointerException();
            }
            V v = super.setValue(value);
            put(getKey(), value);
            return v;
        }
    }

    final class EntryIterator extends HashIterator implements
            ReusableIterator<Entry<K, V>> {
        public Map.Entry<K, V> next() {
            HashEntry<K, V> e = nextEntry();
            return new WriteThroughEntry(e.key(), e.value());
        }
    }

    final class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return ConcurrentWeakKeyHashMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return ConcurrentWeakKeyHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return ConcurrentWeakKeyHashMap.this.remove(o) != null;
        }

        @Override
        public void clear() {
            ConcurrentWeakKeyHashMap.this.clear();
        }
    }

    final class Values extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return ConcurrentWeakKeyHashMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return ConcurrentWeakKeyHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            ConcurrentWeakKeyHashMap.this.clear();
        }
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?>)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            V v = get(e.getKey());
            return v != null && v.equals(e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?>)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return ConcurrentWeakKeyHashMap.this.remove(e.getKey(), e.getValue());
        }

        @Override
        public int size() {
            return ConcurrentWeakKeyHashMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return ConcurrentWeakKeyHashMap.this.isEmpty();
        }

        @Override
        public void clear() {
            ConcurrentWeakKeyHashMap.this.clear();
        }
    }
}