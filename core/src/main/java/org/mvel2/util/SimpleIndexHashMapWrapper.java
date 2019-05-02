package org.mvel2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * As most use-cases of the VariableResolverFactory's rely on Maps, this is meant to implement a simple wrapper
 * which records index positions for use by the optimizing facilities.
 * <p/>
 * This wrapper also ensures that the Map is only additive.  You cannot remove an element once it's been added.
 * While this may seem like an odd limitation, it is consistent with the language semantics. (ie. it's not possible
 * to delete a variable at runtime once it's been declared).
 *
 * @author Mike Brock
 */
public class SimpleIndexHashMapWrapper<K, V> implements Map<K, V> {

    private final Map<K, ValueContainer<K, V>> wrappedMap;
    private final ArrayList<ValueContainer<K, V>> indexBasedLookup;
    private int indexCounter;

    public SimpleIndexHashMapWrapper() {
        this.wrappedMap = new HashMap<K, ValueContainer<K, V>>();
        this.indexBasedLookup = new ArrayList<ValueContainer<K, V>>();
    }

    public SimpleIndexHashMapWrapper(SimpleIndexHashMapWrapper<K, V> wrapper, boolean allocateOnly) {
        this.indexBasedLookup = new ArrayList<ValueContainer<K, V>>(wrapper.indexBasedLookup.size());
        this.wrappedMap = new HashMap<K, ValueContainer<K, V>>();

        ValueContainer<K, V> vc;
        int index = 0;
        if (allocateOnly) {
            for (ValueContainer<K, V> key : wrapper.indexBasedLookup) {
                vc = new ValueContainer<K, V>(index++, key.getKey(), null);
                indexBasedLookup.add(vc);
                wrappedMap.put(key.getKey(), vc);
            }
        } else {
            for (ValueContainer<K, V> key : wrapper.indexBasedLookup) {
                vc = new ValueContainer<K, V>(index++, key.getKey(), key.getValue());
                indexBasedLookup.add(vc);
                wrappedMap.put(key.getKey(), vc);
            }
        }
    }

    public SimpleIndexHashMapWrapper(K[] keys) {
        this.wrappedMap = new HashMap<K, ValueContainer<K, V>>(keys.length * 2);
        this.indexBasedLookup = new ArrayList<ValueContainer<K, V>>(keys.length);

        initWithKeys(keys);
    }

    public SimpleIndexHashMapWrapper(K[] keys, int initialCapacity, float load) {
        this.wrappedMap = new HashMap<K, ValueContainer<K, V>>(initialCapacity * 2, load);
        this.indexBasedLookup = new ArrayList<ValueContainer<K, V>>(initialCapacity);

        initWithKeys(keys);
    }

    public void initWithKeys(K[] keys) {
        int index = 0;
        ValueContainer<K, V> vc;
        for (K key : keys) {
            vc = new ValueContainer<K, V>(index++, key, null);
            wrappedMap.put(key, vc);
            indexBasedLookup.add(vc);
        }
    }

    public void addKey(K key) {
        ValueContainer<K, V> vc = new ValueContainer<K, V>(indexCounter++, key, null);
        this.indexBasedLookup.add(vc);
        this.wrappedMap.put(key, vc);
    }

    public void addKey(K key, V value) {
        ValueContainer<K, V> vc = new ValueContainer<K, V>(indexCounter++, key, value);
        this.indexBasedLookup.add(vc);
        this.wrappedMap.put(key, vc);
    }

    public int size() {
        return wrappedMap.size();
    }

    public boolean isEmpty() {
        return wrappedMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return wrappedMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return wrappedMap.containsValue(value);
    }

    public V get(Object key) {
        return wrappedMap.get(key).getValue();
    }

    public V getByIndex(int index) {
        return indexBasedLookup.get(index).getValue();
    }

    public K getKeyAtIndex(int index) {
        return indexBasedLookup.get(index).getKey();
    }

    public int indexOf(K key) {
        return wrappedMap.get(key).getIndex();
    }

    public V put(K key, V value) {
        ValueContainer<K, V> vc = wrappedMap.get(key);
        if (vc == null) throw new RuntimeException("cannot add a new entry.  you must allocate a new key with addKey() first.");

        indexBasedLookup.add(vc);
        return wrappedMap.put(key, vc).getValue();
    }

    public void putAtIndex(int index, V value) {
        ValueContainer<K, V> vc = indexBasedLookup.get(index);
        vc.setValue(value);
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException("cannot remove keys");
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        //   wrappedMap.put
    }

    public void clear() {
        throw new UnsupportedOperationException("cannot clear map");
    }

    public Set<K> keySet() {
        return wrappedMap.keySet();
    }

    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    private class ValueContainer<K, V> {

        private int index;
        private K key;
        private V value;

        public ValueContainer(int index, K key, V value) {
            this.index = index;
            this.key = key;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public K getKey() {
            return key;
        }

        void setKey(K key) {
            this.key = key;
        }

        public V getValue() {
            return value;
        }

        void setValue(V value) {
            this.value = value;
        }
    }
}
