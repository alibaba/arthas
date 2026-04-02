package com.taobao.arthas.core.shell.term.impl.http.session;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;

/**
 * 基于 LinkedHashMap 的 LRU（最近最少使用）缓存实现。
 *
 * <p>
 * 该缓存具有固定的最大元素数量（cacheSize）。
 * 如果缓存已满且添加了新条目，则会删除 LRU（最近最少使用）的条目。
 *
 * <p>
 * 此类是线程安全的。所有方法都已同步。
 *
 * <p>
 * 作者: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland<br>
 * 多重许可: EPL / LGPL / GPL / AL / BSD.
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class LRUCache<K, V> {

    /**
     * 哈希表负载因子，用于计算哈希表的初始容量。
     * 负载因子决定了哈希表在何时进行扩容。
     * 0.75 是一个经验值，在时间和空间成本之间提供了良好的权衡。
     */
    private static final float hashTableLoadFactor = 0.75f;

    /**
     * 底层使用 LinkedHashMap 存储缓存数据。
     * LinkedHashMap 维护了插入顺序或访问顺序，这是实现 LRU 的关键。
     */
    private LinkedHashMap<K, V> map;

    /**
     * 缓存的最大容量。
     * 当缓存中的条目数超过此值时，最久未使用的条目将被移除。
     */
    private int cacheSize;

    /**
     * 创建一个新的 LRU 缓存。
     *
     * @param cacheSize 缓存中要保留的最大条目数
     */
    public LRUCache(int cacheSize) {
        // 保存缓存的最大容量
        this.cacheSize = cacheSize;

        // 计算哈希表的初始容量。
        // 除以负载因子并向上取整，再加1，确保在达到最大容量之前不会扩容。
        // 例如：cacheSize=100, loadFactor=0.75, capacity=100/0.75+1=134
        int hashTableCapacity = (int) Math.ceil(cacheSize / hashTableLoadFactor) + 1;

        // 创建一个自定义的 LinkedHashMap 实例。
        // 参数说明：
        // - hashTableCapacity: 初始容量
        // - hashTableLoadFactor: 负载因子
        // - true: 启用访问顺序模式（按访问顺序迭代，而非插入顺序）
        map = new LinkedHashMap<K, V>(hashTableCapacity, hashTableLoadFactor, true) {
            // （一个匿名内部类）
            private static final long serialVersionUID = 1;

            /**
             * 在插入新条目后由 LinkedHashMap 调用，用于判断是否应移除最老的条目。
             * 这是实现 LRU 策略的核心方法。
             *
             * @param eldest 最老的条目（即最少使用的条目）
             * @return 如果应移除最老的条目，则返回 true
             */
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                // 当缓存大小超过设定的最大容量时，移除最老的条目
                return size() > LRUCache.this.cacheSize;
            }
        };
    }

    /**
     * 从缓存中检索一个条目。
     * <br>
     * 被检索的条目将成为 MRU（最近使用）的条目。
     * 由于 LinkedHashMap 启用了访问顺序模式，访问一个条目会将其移到队列的末尾。
     *
     * @param key 要返回其关联值的键
     * @return 与此键关联的值；如果缓存中不存在具有此键的值，则返回 null
     */
    public synchronized V get(K key) {
        // 从底层 map 中获取值
        // LinkedHashMap 会自动更新访问顺序，将此条目移到末尾
        return map.get(key);
    }

    /**
     * 向此缓存添加一个条目。
     * 新条目将成为 MRU（最近使用）的条目。
     * 如果缓存中已存在具有指定键的条目，则将其替换为新条目。
     * 如果缓存已满，将自动删除 LRU（最近最少使用）的条目。
     *
     * @param key   要与指定值关联的键
     * @param value 要与指定键关联的值
     */
    public synchronized void put(K key, V value) {
        // 将条目放入底层 map
        // LinkedHashMap 会：
        // 1. 如果键已存在，更新值并将条目移到末尾
        // 2. 如果键不存在，添加新条目到末尾
        // 3. 如果添加后超过容量，removeEldestEntry() 返回 true，自动删除最老的条目
        map.put(key, value);
    }

    /**
     * 清空缓存。
     * 删除缓存中的所有条目。
     */
    public synchronized void clear() {
        // 清空底层 map，删除所有条目
        map.clear();
    }

    /**
     * 返回缓存中已使用的条目数。
     *
     * @return 当前缓存中的条目数量
     */
    public synchronized int usedEntries() {
        // 返回底层 map 的大小，即当前缓存的条目数
        return map.size();
    }

    /**
     * 返回包含所有缓存条目副本的集合。
     * 返回的是一个新创建的 ArrayList，包含所有缓存条目的快照。
     *
     * @return 包含缓存内容副本的集合
     */
    public synchronized Collection<Map.Entry<K, V>> getAll() {
        // 创建一个新的 ArrayList，包含当前所有缓存条目的副本
        // 这样可以确保返回的集合不受后续缓存修改的影响
        return new ArrayList<Map.Entry<K, V>>(map.entrySet());
    }

} // LRUCache 类结束
