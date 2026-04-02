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
import java.util.Arrays;
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
 * 一个基于弱键（weak-key）的并发映射表，类似于 {@link ConcurrentHashMap}。
 *
 * <p>该类实现了一个线程安全的弱引用键哈希映射表，使用分段锁（segmented locking）
 * 技术来支持高并发访问。键（key）使用弱引用存储，当键不再被外部引用时，
 * 可以被垃圾回收器自动回收。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>键使用弱引用，允许自动垃圾回收</li>
 *   <li>使用分段锁机制提供高并发性能</li>
 *   <li>支持无锁读取操作</li>
 *   <li>自动清理过期的弱引用</li>
 * </ul>
 *
 * @param <K> 此映射维护的键类型
 * @param <V> 映射值的类型
 */
public final class ConcurrentWeakKeyHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    /*
     * 基本策略是将哈希表划分为多个段（Segments），
     * 每个段本身就是一个可并发读写的哈希表。
     */

    /**
     * 此表的默认初始容量，当构造函数中未指定时使用。
     * 默认为16，即表初始包含16个桶（bucket）。
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * 此表的默认负载因子，当构造函数中未指定时使用。
     * 负载因子用于控制哈希表的扩容时机，当元素数量达到容量与负载因子的乘积时触发扩容。
     * 默认值为0.75，这是一个在时间和空间成本上权衡良好的值。
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 此表的默认并发级别，当构造函数中未指定时使用。
     * 并发级别决定了表中段（Segment）的数量，影响并发更新的性能。
     * 默认值为16，表示支持最多16个并发写入线程。
     */
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 表的最大容量。如果构造函数隐式指定了更高的值，则使用此值。
     * 必须是2的幂次方且小于等于 1<<30，以确保条目可以使用整数进行索引。
     * 最大容量为 2^30 = 1073741824。
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 允许的最大段数，用于限制构造函数参数。
     * 最大值为 2^16 = 65536，这个值相对保守，确保在大多数实际场景中足够使用。
     */
    static final int MAX_SEGMENTS = 1 << 16; // 略微保守的值

    /**
     * 在size()和containsValue()方法中，在加锁之前尝试无同步重试的次数。
     * 这用于避免在表持续修改的情况下进行无限重试，因为在持续修改的场景下
     * 无法获得准确的结果。最多重试2次后就会使用加锁方式。
     */
    static final int RETRIES_BEFORE_LOCK = 2;

    /* ---------------- 字段 -------------- */

    /**
     * 用于索引到段的掩码值。
     * 键哈希码的高位用于选择段，这个掩码用于提取这些位。
     * 例如，如果segmentMask为15（二进制1111），则使用哈希码的低4位来选择段。
     */
    final int segmentMask;

    /**
     * 用于在段内索引的移位值。
     * 这个值用于将哈希码右移，以便与segmentMask一起使用来选择正确的段。
     * 移位值的计算方式是：32 - log2(段数)。
     */
    final int segmentShift;

    /**
     * 段数组，每个段本身就是一个专门的哈希表。
     * 表被划分为多个段，每个段维护自己的哈希表和锁，
     * 这样可以支持多个线程并发访问不同的段，提高并发性能。
     */
    final Segment<K, V>[] segments;

    // 键集合的缓存视图，延迟初始化
    Set<K> keySet;
    // 条目集合的缓存视图，延迟初始化
    Set<Map.Entry<K, V>> entrySet;
    // 值集合的缓存视图，延迟初始化
    Collection<V> values;

    /* ---------------- 小工具方法 -------------- */

    /**
     * 对给定的哈希码应用补充哈希函数，用于防御低质量的哈希函数。
     *
     * <p>这是至关重要的，因为ConcurrentReferenceHashMap使用2的幂次方长度的哈希表，
     * 否则在哈希码的低位或高位不不同的情况下会遇到碰撞。
     * 这个函数通过混合哈希码的位来减少碰撞概率。</p>
     *
     * @param h 原始哈希码
     * @return 经过补充哈希处理后的哈希码
     */
    private static int hash(int h) {
        // 扩散位以规范化段和索引位置，
        // 使用单字Wang/Jenkins哈希的变体。
        h += h << 15 ^ 0xffffcd7d;
        h ^= h >>> 10;
        h += h << 3;
        h ^= h >>> 6;
        h += (h << 2) + (h << 14);
        return h ^ h >>> 16;
    }

    /**
     * 返回应该用于具有给定哈希码的键的段。
     *
     * @param hash 键的哈希码
     * @return 应该包含该键的段
     */
    Segment<K, V> segmentFor(int hash) {
        // 使用哈希码的高位来选择段
        return segments[hash >>> segmentShift & segmentMask];
    }

    /**
     * 计算对象的哈希码，并应用补充哈希函数。
     *
     * @param key 要计算哈希的对象
     * @return 经过处理的哈希码
     */
    private static int hashOf(Object key) {
        return hash(key.hashCode());
    }

    /* ---------------- 内部类 -------------- */

    /**
     * 弱键引用，存储回收所需的键哈希码。
     *
     * <p>这个类扩展了WeakReference，用于存储键的弱引用。
     * 当键不再被外部引用时，它可以被垃圾回收器自动回收。
     * 同时保存了键的哈希码，以便在回收后仍能定位到对应的哈希表位置。</p>
     */
    static final class WeakKeyReference<K> extends WeakReference<K> {

        // 键的哈希码，在键被回收后仍保留用于定位
        final int hash;

        /**
         * 创建一个新的弱键引用。
         *
         * @param key 被引用的键对象
         * @param hash 键的哈希码
         * @param refQueue 引用队列，用于通知引用被回收
         */
        WeakKeyReference(K key, int hash, ReferenceQueue<Object> refQueue) {
            super(key, refQueue);
            this.hash = hash;
        }

        /**
         * 返回键的哈希码。
         *
         * @return 键的哈希码
         */
        public int keyHash() {
            return hash;
        }

        /**
         * 返回引用对象本身。
         *
         * @return 此引用对象
         */
        public Object keyRef() {
            return this;
        }
    }

    /**
     * ConcurrentReferenceHashMap的链表条目。
     *
     * <p>注意：这个类永远不会作为用户可见的Map.Entry导出。</p>
     *
     * <p>因为value字段是volatile而非final的，根据Java内存模型，
     * 在数据竞争中读取时，未同步的读者可能看到null而不是初始值。
     * 虽然导致这种情况的重排序不太可能实际发生，
     * 但Segment.readValueUnderLock方法作为备份，
     * 以防在未同步访问方法中看到null（预初始化）值。</p>
     */
    static final class HashEntry<K, V> {
        // 键的引用对象（WeakKeyReference），包含弱引用
        final Object keyRef;
        // 键的哈希码，不可变
        final int hash;
        // 值的引用，使用volatile确保可见性
        volatile Object valueRef;
        // 链表中的下一个节点，不可变
        final HashEntry<K, V> next;

        /**
         * 创建一个新的哈希条目。
         *
         * @param key 键对象
         * @param hash 键的哈希码
         * @param next 链表中的下一个节点
         * @param value 值对象
         * @param refQueue 引用队列，用于注册弱引用
         */
        HashEntry(
                K key, int hash, HashEntry<K, V> next, V value,
                ReferenceQueue<Object> refQueue) {
            this.hash = hash;
            this.next = next;
            // 创建键的弱引用并注册到引用队列
            keyRef = new WeakKeyReference<K>(key, hash, refQueue);
            valueRef = value;
        }

        /**
         * 获取键对象。
         * 由于键是弱引用，可能已经被回收。
         *
         * @return 键对象，如果已被回收则返回null
         */
        @SuppressWarnings("unchecked")
        K key() {
            return ((Reference<K>) keyRef).get();
        }

        /**
         * 获取值对象。
         *
         * @return 值对象
         */
        V value() {
            return dereferenceValue(valueRef);
        }

        /**
         * 解引用值对象。
         * 如果值是弱引用，则获取其实际值；否则直接返回值。
         *
         * @param value 值引用对象
         * @return 实际的值对象
         */
        @SuppressWarnings("unchecked")
        V dereferenceValue(Object value) {
            if (value instanceof WeakKeyReference) {
                return ((Reference<V>) value).get();
            }

            return (V) value;
        }

        /**
         * 设置值对象。
         *
         * @param value 新的值对象
         */
        void setValue(V value) {
            valueRef = value;
        }

        /**
         * 创建指定大小的新哈希条目数组。
         *
         * @param i 数组大小
         * @return 新的哈希条目数组
         */
        @SuppressWarnings("unchecked")
        static <K, V> HashEntry<K, V>[] newArray(int i) {
            return new HashEntry[i];
        }
    }

    /**
     * 段是哈希表的专门版本。
     *
     * <p>这个类继承自ReentrantLock，只是为了简化一些锁操作并避免单独构造。
     * 每个段维护自己的哈希表，可以独立进行加锁，从而支持并发访问。</p>
     *
     * <p>段的主要特性：</p>
     * <ul>
     *   <li>维护一个条目链表数组，始终保持一致状态</li>
     *   <li>支持无锁读取操作</li>
     *   <li>写操作需要获取锁</li>
     *   <li>自动清理过期的弱引用</li>
     * </ul>
     */
    static final class Segment<K, V> extends ReentrantLock {
        /*
         * 段维护一个条目链表数组，这些数组始终保持一致状态，因此可以无锁读取。
         * 节点的next字段是不可变的（final）。所有链表添加操作都在每个桶的前面执行。
         * 这使得检查变更变得容易，遍历也很快。当节点需要变更时，会创建新节点来替换它们。
         * 这对哈希表很有效，因为桶链表通常很短。（对于默认负载因子阈值，
         * 平均长度小于2。）
         *
         * 因此，读操作可以在不加锁的情况下进行，但依赖于对volatile的精选使用，
         * 以确保注意到其他线程完成的写操作。对于大多数目的，
         * 跟踪元素数量的"count"字段作为确保可见性的volatile变量。
         * 这很方便，因为许多读操作无论如何都需要读取此字段：
         *
         *   - 所有（未同步的）读操作必须首先读取"count"字段，
         *     如果为0，则不应查看表条目。
         *
         *   - 所有（同步的）写操作应在结构性改变任何桶后写入"count"字段。
         *     操作不得采取任何可能甚至暂时导致并发读操作看到不一致数据的操作。
         *     由于Map中读操作的性质，这变得更容易。例如，没有操作可以揭示
         *     表已增长但阈值尚未更新的情况，因此对此没有原子性要求。
         *
         * 作为指导，所有对count字段的关键volatile读写都在代码注释中标记。
         */

        private static final long serialVersionUID = -8328104880676891126L;

        /**
         * 此段区域中的元素数量。
         * 使用volatile修饰确保在多线程环境下的可见性。
         * 读操作依赖此字段的volatile读来确保看到写操作的完成。
         */
        transient volatile int count;

        /**
         * 改变表大小的更新次数。
         * 这在批量读取方法中使用，以确保它们看到一致的状态快照：
         * 如果在遍历段计算大小或检查containsValue时modCount发生变化，
         * 那么我们可能看到不一致的状态视图，因此（通常）必须重试。
         */
        int modCount;

        /**
         * 当表大小超过此阈值时，表将进行重新哈希（扩容）。
         * 此字段的值始终为 (capacity * loadFactor)。
         * 当元素数量达到此阈值时，会触发扩容操作。
         */
        int threshold;

        /**
         * 每个段的哈希表。
         * 这是一个数组，每个元素是一个链表的头节点。
         * 使用volatile修饰确保在多线程环境下的可见性。
         */
        transient volatile HashEntry<K, V>[] table;

        /**
         * 哈希表的负载因子。
         * 尽管此值对所有段都是相同的，但它被复制以避免需要链接到外部对象。
         * 负载因子决定了哈希表何时扩容。
         */
        final float loadFactor;

        /**
         * 此段收集的弱键引用队列。
         * 每当分配表时，应该（重新）初始化此队列。
         * 当弱引用的对象被垃圾回收时，GC会将引用放入此队列，
         * 以便我们可以清理过期的条目。
         */
        transient volatile ReferenceQueue<Object> refQueue;

        /**
         * 创建一个新段。
         *
         * @param initialCapacity 初始容量
         * @param lf 负载因子
         */
        Segment(int initialCapacity, float lf) {
            loadFactor = lf;
            // 初始化哈希表和引用队列
            setTable(HashEntry.<K, V>newArray(initialCapacity));
        }

        /**
         * 创建指定大小的新段数组。
         *
         * @param i 数组大小
         * @return 新的段数组
         */
        @SuppressWarnings("unchecked")
        static <K, V> Segment<K, V>[] newArray(int i) {
            return new Segment[i];
        }

        /**
         * 比较两个键对象是否相等。
         *
         * @param src 源键对象
         * @param dest 目标键对象
         * @return 如果键相等则返回true，否则返回false
         */
        private static boolean keyEq(Object src, Object dest) {
            return src.equals(dest);
        }

        /**
         * 将表设置为新的哈希条目数组。
         * 仅在持有锁或在构造函数中调用此方法。
         * 同时初始化阈值和引用队列。
         *
         * @param newTable 新的哈希表数组
         */
        void setTable(HashEntry<K, V>[] newTable) {
            // 计算扩容阈值
            threshold = (int) (newTable.length * loadFactor);
            table = newTable;
            // 创建新的引用队列，用于收集过期的弱引用
            refQueue = new ReferenceQueue<Object>();
        }

        /**
         * 返回给定哈希码对应的桶的第一个条目。
         *
         * @param hash 键的哈希码
         * @return 桶中的第一个条目，如果桶为空则返回null
         */
        HashEntry<K, V> getFirst(int hash) {
            HashEntry<K, V>[] tab = table;
            // 使用哈希码的低位作为索引
            return tab[hash & tab.length - 1];
        }

        /**
         * 创建一个新的哈希条目，使用此段的引用队列。
         *
         * @param key 键对象
         * @param hash 哈希码
         * @param next 链表中的下一个节点
         * @param value 值对象
         * @return 新创建的哈希条目
         */
        HashEntry<K, V> newHashEntry(
                K key, int hash, HashEntry<K, V> next, V value) {
            return new HashEntry<K, V>(
                    key, hash, next, value, refQueue);
        }

        /**
         * 在锁保护下读取条目的值字段。
         * 当值字段看起来为null时调用此方法。
         * 这只有在编译器碰巧重排序了HashEntry初始化和表赋值时才可能，
         * 这在内存模型下是合法的，但不知道是否实际发生过。
         *
         * @param e 要读取的哈希条目
         * @return 条目的值
         */
        V readValueUnderLock(HashEntry<K, V> e) {
            lock();
            try {
                // 清理过期的条目
                removeStale();
                return e.value();
            } finally {
                unlock();
            }
        }

        /* 映射方法的专门实现 */

        /**
         * 从此段中获取与键关联的值。
         *
         * @param key 要查找的键
         * @param hash 键的哈希码
         * @return 与键关联的值，如果不存在则返回null
         */
        V get(Object key, int hash) {
            if (count != 0) { // read-volatile - 必须首先读取volatile字段
                HashEntry<K, V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && keyEq(key, e.key())) {
                        Object opaque = e.valueRef;
                        if (opaque != null) {
                            return e.dereferenceValue(opaque);
                        }

                        // 如果值为null，重新检查（可能是重排序导致的）
                        return readValueUnderLock(e); // recheck
                    }
                    e = e.next;
                }
            }
            return null;
        }

        /**
         * 检查此段是否包含指定的键。
         *
         * @param key 要查找的键
         * @param hash 键的哈希码
         * @return 如果此段包含指定的键则返回true，否则返回false
         */
        boolean containsKey(Object key, int hash) {
            if (count != 0) { // read-volatile - 必须首先读取volatile字段
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

        /**
         * 检查此段是否将一个或多个键映射到指定值。
         *
         * @param value 要查找的值
         * @return 如果此段包含指定的值则返回true，否则返回false
         */
        boolean containsValue(Object value) {
            if (count != 0) { // read-volatile - 必须首先读取volatile字段
                for (HashEntry<K, V> e: table) {
                    for (; e != null; e = e.next) {
                        Object opaque = e.valueRef;
                        V v;

                        if (opaque == null) {
                            // 如果值为null，重新检查
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

        /**
         * 仅当当前映射到指定值时，才替换指定键的条目。
         *
         * @param key 要替换的键
         * @param hash 键的哈希码
         * @param oldValue 预期的当前值
         * @param newValue 要设置的新值
         * @return 如果值被替换则返回true，否则返回false
         */
        boolean replace(K key, int hash, V oldValue, V newValue) {
            lock();
            try {
                // 清理过期的条目
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

        /**
         * 仅当键当前映射到某个值时，才替换该条目。
         *
         * @param key 要替换的键
         * @param hash 键的哈希码
         * @param newValue 要设置的新值
         * @return 与键关联的先前值，如果没有则返回null
         */
        V replace(K key, int hash, V newValue) {
            lock();
            try {
                // 清理过期的条目
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

        /**
         * 将键映射到此段中的值。
         *
         * @param key 要映射的键
         * @param hash 键的哈希码
         * @param value 要映射的值
         * @param onlyIfAbsent 如果为true，则仅当键不存在时才映射
         * @return 与键关联的先前值，如果没有则返回null
         */
        V put(K key, int hash, V value, boolean onlyIfAbsent) {
            lock();
            try {
                // 清理过期的条目
                removeStale();
                int c = count;
                if (c ++ > threshold) { // ensure capacity - 确保容量
                    // 如果需要，进行扩容
                    int reduced = rehash();
                    if (reduced > 0) {
                        count = (c -= reduced) - 1; // write-volatile - 必须写入volatile字段
                    }
                }

                HashEntry<K, V>[] tab = table;
                int index = hash & tab.length - 1;
                HashEntry<K, V> first = tab[index];
                HashEntry<K, V> e = first;
                // 查找是否已存在该键
                while (e != null && (e.hash != hash || !keyEq(key, e.key()))) {
                    e = e.next;
                }

                V oldValue;
                if (e != null) {
                    // 键已存在，更新值
                    oldValue = e.value();
                    if (!onlyIfAbsent) {
                        e.setValue(value);
                    }
                } else {
                    // 键不存在，添加新条目
                    oldValue = null;
                    ++ modCount;
                    // 在链表头部插入新条目
                    tab[index] = newHashEntry(key, hash, first, value);
                    count = c; // write-volatile - 必须写入volatile字段
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        /**
         * 扩容并重新哈希表。
         * 将表大小加倍，并将所有有效条目重新分配到新表中。
         *
         * @return 由于垃圾回收而移除的过期条目数量
         */
        int rehash() {
            HashEntry<K, V>[] oldTable = table;
            int oldCapacity = oldTable.length;
            // 如果已经达到最大容量，不再扩容
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                return 0;
            }

            /*
             * 将每个链表中的节点重新分类到新表。因为我们使用2的幂次方扩容，
             * 所以每个桶中的元素必须要么保持在相同的索引，要么以2的幂次方偏移量移动。
             * 我们通过捕获旧节点可以重用的情况来消除不必要的节点创建，
             * 因为它们的next字段不会改变。统计上，在默认阈值下，
             * 当表加倍时只有大约六分之一的节点需要克隆。
             * 它们替换的节点将可以被垃圾回收，只要它们不再被任何可能正在遍历表的读者线程引用。
             */

            HashEntry<K, V>[] newTable = HashEntry.newArray(oldCapacity << 1);
            threshold = (int) (newTable.length * loadFactor);
            int sizeMask = newTable.length - 1;
            int reduce = 0;
            for (HashEntry<K, V> e: oldTable) {
                // 我们需要保证对旧表的任何现有读取都可以继续。
                // 所以我们还不能将每个桶置为null。
                if (e != null) {
                    HashEntry<K, V> next = e.next;
                    int idx = e.hash & sizeMask;

                    // 链表中只有一个节点
                    if (next == null) {
                        newTable[idx] = e;
                    } else {
                        // 重用同一槽位的尾随连续序列
                        HashEntry<K, V> lastRun = e;
                        int lastIdx = idx;
                        // 找到最后一个需要移动位置的节点
                        for (HashEntry<K, V> last = next; last != null; last = last.next) {
                            int k = last.hash & sizeMask;
                            if (k != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }
                        newTable[lastIdx] = lastRun;
                        // 克隆所有剩余节点
                        for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {
                            // 跳过已被垃圾回收的弱引用
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
         * 从此段中移除条目。
         * 如果value为null，则只匹配键；否则同时匹配键和值。
         *
         * @param key 要移除的键
         * @param hash 键的哈希码
         * @param value 要匹配的值（可以为null）
         * @param refRemove 是否为引用移除操作
         * @return 与键关联的先前值，如果没有匹配则返回null
         */
        V remove(Object key, int hash, Object value, boolean refRemove) {
            lock();
            try {
                if (!refRemove) {
                    // 如果不是引用移除操作，先清理过期条目
                    removeStale();
                }
                int c = count - 1;
                HashEntry<K, V>[] tab = table;
                int index = hash & tab.length - 1;
                HashEntry<K, V> first = tab[index];
                HashEntry<K, V> e = first;
                // 引用移除操作比较引用实例，键移除操作比较键对象
                while (e != null && key != e.keyRef &&
                        (refRemove || hash != e.hash || !keyEq(key, e.key()))) {
                    e = e.next;
                }

                V oldValue = null;
                if (e != null) {
                    V v = e.value();
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        // 被移除节点之后的所有条目可以保留在链表中，
                        // 但之前的所有条目都需要克隆。
                        ++ modCount;
                        HashEntry<K, V> newFirst = e.next;
                        for (HashEntry<K, V> p = first; p != e; p = p.next) {
                            K pKey = p.key();
                            if (pKey == null) { // Skip GC'd keys - 跳过已被垃圾回收的键
                                c --;
                                continue;
                            }

                            // 创建新节点
                            newFirst = newHashEntry(
                                    pKey, p.hash, newFirst, p.value());
                        }
                        tab[index] = newFirst;
                        count = c; // write-volatile - 必须写入volatile字段
                    }
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        /**
         * 从此段中移除所有过期的弱引用条目。
         * 此方法会处理引用队列中的所有过期引用，并从哈希表中移除对应的条目。
         */
        @SuppressWarnings("rawtypes")
        void removeStale() {
            WeakKeyReference ref;
            // 从引用队列中取出所有过期的弱引用，并移除对应的条目
            while ((ref = (WeakKeyReference) refQueue.poll()) != null) {
                remove(ref.keyRef(), ref.keyHash(), null, true);
            }
        }

        /**
         * 清空此段中的所有条目。
         */
        void clear() {
            if (count != 0) {
                lock();
                try {
                    // 清空哈希表
                    Arrays.fill(table, null);
                    ++ modCount;
                    // 替换引用队列以避免不必要的过期清理
                    refQueue = new ReferenceQueue<Object>();
                    count = 0; // write-volatile - 必须写入volatile字段
                } finally {
                    unlock();
                }
            }
        }
    }

    /* ---------------- 公共操作 -------------- */

    /**
     * 创建一个新的、空的映射，具有指定的初始容量、负载因子和并发级别。
     *
     * @param initialCapacity 初始容量。实现执行内部大小调整以容纳这么多元素。
     * @param loadFactor 负载因子阈值，用于控制调整大小。
     *                   当每个桶的平均元素数超过此阈值时，可能会执行调整大小。
     * @param concurrencyLevel 预估的并发更新线程数。实现执行内部大小调整
     *                         以尝试容纳这么多线程。
     * @throws IllegalArgumentException 如果初始容量为负数或负载因子或并发级别非正
     */
    public ConcurrentWeakKeyHashMap(
            int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }

        if (concurrencyLevel > MAX_SEGMENTS) {
            concurrencyLevel = MAX_SEGMENTS;
        }

        // 找到最匹配参数的2的幂次方大小
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++ sshift;
            ssize <<= 1;
        }
        // 计算段移位和段掩码
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        segments = Segment.newArray(ssize);

        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        // 计算每个段的初始容量
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity) {
            ++ c;
        }
        // 确保容量是2的幂次方
        int cap = 1;
        while (cap < c) {
            cap <<= 1;
        }

        // 初始化所有段
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
         * 我们跟踪每个段的modCount以避免ABA问题，
         * 即在一个段中添加元素而在另一个段中删除元素，
         * 在这种情况下，表在任何时候实际上都不是空的。
         * 注意在size()和containsValue()方法中modCount的类似使用，
         * 这些是唯一其他容易受到ABA问题影响的方法。
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
        // 如果mcsum恰好为零，那么我们知道在进行任何修改之前得到了快照。
        // 这可能足够常见，值得跟踪。
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
     * 移除键已被终结的所有过期条目。
     *
     * <p>通常不需要使用此方法，因为过期条目会在需要阻塞操作时延迟自动移除。
     * 然而，在某些情况下应该主动执行此操作，例如在多类加载器环境中清理对类加载器的旧引用。</p>
     *
     * <p>注意：此方法将在表的所有段上逐个获取锁，因此如果要使用，应谨慎使用。</p>
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

    /* ---------------- 迭代器支持 -------------- */

    /**
     * 哈希迭代器的基类。
     *
     * <p>此类提供了遍历并发弱键哈希映射的基本功能。
     * 迭代器是弱一致性的，永远不会抛出ConcurrentModificationException。</p>
     */
    abstract class HashIterator {
        // 下一个要访问的段的索引
        int nextSegmentIndex;
        // 当前表中下一个要访问的桶的索引
        int nextTableIndex;
        // 当前正在遍历的哈希表
        HashEntry<K, V>[] currentTable;
        // 下一个要返回的条目
        HashEntry<K, V> nextEntry;
        // 最后返回的条目，用于remove操作
        HashEntry<K, V> lastReturned;
        // 当前键的强引用，防止在迭代过程中被垃圾回收
        K currentKey; // Strong reference to weak key (prevents gc)

        /**
         * 创建一个新的哈希迭代器。
         */
        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        /**
         * 将迭代器重置到起始位置。
         */
        public void rewind() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            currentTable = null;
            nextEntry = null;
            lastReturned = null;
            currentKey = null;
            advance();
        }

        /**
         * 检查是否还有更多元素。
         *
         * @return 如果还有更多元素则返回true，否则返回false
         */
        public boolean hasMoreElements() {
            return hasNext();
        }

        /**
         * 推进到下一个有效条目。
         * 此方法会跳过所有已被垃圾回收的弱引用键。
         */
        final void advance() {
            // 尝试移动到当前链表的下一个节点
            if (nextEntry != null && (nextEntry = nextEntry.next) != null) {
                return;
            }

            // 在当前表中查找下一个非空桶
            while (nextTableIndex >= 0) {
                if ((nextEntry = currentTable[nextTableIndex --]) != null) {
                    return;
                }
            }

            // 在其他段中查找下一个非空表
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

        /**
         * 检查是否还有更多元素。
         *
         * @return 如果还有更多元素则返回true，否则返回false
         */
        public boolean hasNext() {
            while (nextEntry != null) {
                // 确保键没有被垃圾回收
                if (nextEntry.key() != null) {
                    return true;
                }
                advance();
            }

            return false;
        }

        /**
         * 返回下一个哈希条目。
         *
         * @return 下一个哈希条目
         * @throws NoSuchElementException 如果没有更多元素
         */
        HashEntry<K, V> nextEntry() {
            do {
                if (nextEntry == null) {
                    throw new NoSuchElementException();
                }

                lastReturned = nextEntry;
                currentKey = lastReturned.key();
                advance();
            } while (currentKey == null); // Skip GC'd keys - 跳过已被垃圾回收的键

            return lastReturned;
        }

        /**
         * 从映射中移除最后返回的条目。
         *
         * @throws IllegalStateException 如果尚未调用next或已经调用了remove
         */
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            ConcurrentWeakKeyHashMap.this.remove(currentKey);
            lastReturned = null;
        }
    }

    /**
     * 键迭代器。
     * 遍历映射中的所有键，跳过已被垃圾回收的弱引用键。
     */
    final class KeyIterator
            extends HashIterator implements ReusableIterator<K>, Enumeration<K> {

        /**
         * 返回下一个键。
         *
         * @return 下一个键
         */
        public K next() {
            return nextEntry().key();
        }

        /**
         * 返回下一个键（作为枚举器）。
         *
         * @return 下一个键
         */
        public K nextElement() {
            return nextEntry().key();
        }
    }

    /**
     * 值迭代器。
     * 遍历映射中的所有值。
     */
    final class ValueIterator
            extends HashIterator implements ReusableIterator<V>, Enumeration<V> {

        /**
         * 返回下一个值。
         *
         * @return 下一个值
         */
        public V next() {
            return nextEntry().value();
        }

        /**
         * 返回下一个值（作为枚举器）。
         *
         * @return 下一个值
         */
        public V nextElement() {
            return nextEntry().value();
        }
    }

    /*
     * 这个类是为了JDK5兼容性而需要的。
     * 这是一个简单的Map.Entry实现，保存键值对。
     */
    static class SimpleEntry<K, V> implements Entry<K, V> {

        // 键对象，不可变
        private final K key;

        // 值对象，可变
        private V value;

        /**
         * 创建一个新的条目。
         *
         * @param key 键对象
         * @param value 值对象
         */
        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * 从现有条目创建一个新条目。
         *
         * @param entry 现有条目
         */
        public SimpleEntry(Entry<? extends K, ? extends V> entry) {
            key = entry.getKey();
            value = entry.getValue();
        }

        /**
         * 返回与此条目对应的键。
         *
         * @return 键对象
         */
        public K getKey() {
            return key;
        }

        /**
         * 返回与此条目对应的值。
         *
         * @return 值对象
         */
        public V getValue() {
            return value;
        }

        /**
         * 将值替换为指定值。
         *
         * @param value 新的值
         * @return 先前的值
         */
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

        /**
         * 比较两个对象是否相等。
         *
         * @param o1 第一个对象
         * @param o2 第二个对象
         * @return 如果对象相等则返回true，否则返回false
         */
        private static boolean eq(Object o1, Object o2) {
            return o1 == null? o2 == null : o1.equals(o2);
        }
    }

    /**
     * 自定义条目类，由EntryIterator.next()使用，
     * 将setValue更改传递到底层映射。
     *
     * <p>当通过迭代器修改条目的值时，更改会自动反映到底层映射中。</p>
     */
    final class WriteThroughEntry extends SimpleEntry<K, V> {

        /**
         * 创建一个新的写入条目。
         *
         * @param k 键对象
         * @param v 值对象
         */
        WriteThroughEntry(K k, V v) {
            super(k, v);
        }

        /**
         * 设置条目的值并写入到映射中。
         *
         * <p>要返回的值在这里有些随意。由于WriteThroughEntry不一定跟踪异步更改，
         * 最新的"先前"值可能与我们返回的值不同（甚至可能已被删除，
         * 在这种情况下put将重新建立）。我们不能也不保证更多。</p>
         *
         * @param value 要设置的新值
         * @return 先前的值
         * @throws NullPointerException 如果指定的值为null
         */
        @Override
        public V setValue(V value) {

            if (value == null) {
                throw new NullPointerException();
            }
            // 更新本地值
            V v = super.setValue(value);
            // 将更改写入到底层映射
            put(getKey(), value);
            return v;
        }
    }

    /**
     * 条目迭代器。
     * 遍历映射中的所有条目。
     */
    final class EntryIterator extends HashIterator implements
            ReusableIterator<Entry<K, V>> {
        /**
         * 返回下一个条目。
         *
         * @return 下一个映射条目
         */
        public Map.Entry<K, V> next() {
            HashEntry<K, V> e = nextEntry();
            return new WriteThroughEntry(e.key(), e.value());
        }
    }

    /**
     * 键集合视图。
     * 提供对映射中所有键的集合视图。
     */
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

    /**
     * 值集合视图。
     * 提供对映射中所有值的集合视图。
     */
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

    /**
     * 条目集合视图。
     * 提供对映射中所有条目的集合视图。
     */
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