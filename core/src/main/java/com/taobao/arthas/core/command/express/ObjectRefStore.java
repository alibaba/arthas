package com.taobao.arthas.core.command.express;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ObjectRefStore {
    private static final int DEFAULT_MAX_ENTRIES = 1024;
    private static final String DEFAULT_NAMESPACE = "default";

    private static final ObjectRefStore INSTANCE = new ObjectRefStore(DEFAULT_MAX_ENTRIES);

    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
    private final LinkedHashMap<StoreKey, Entry> entries;
    private final int maxEntries;
    private final AtomicLong idGenerator = new AtomicLong(0);

    private final Ref rootRef;

    private ObjectRefStore(int maxEntries) {
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<StoreKey, Entry>(16, 0.75f, true);
        this.rootRef = new Ref(this, DEFAULT_NAMESPACE);
    }

    public static Ref ref() {
        return INSTANCE.rootRef;
    }

    private synchronized void drainReferenceQueue() {
        Reference<? extends Object> reference;
        while ((reference = referenceQueue.poll()) != null) {
            if (!(reference instanceof KeyedWeakReference)) {
                continue;
            }
            KeyedWeakReference keyedReference = (KeyedWeakReference) reference;
            Entry current = entries.get(keyedReference.key);
            if (current != null && current.id == keyedReference.id) {
                entries.remove(keyedReference.key);
            }
        }
    }

    private static String normalizeNamespace(String namespace) {
        if (namespace == null) {
            return DEFAULT_NAMESPACE;
        }
        String trimmed = namespace.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_NAMESPACE;
        }
        return trimmed;
    }

    private static String requireKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("key is blank");
        }
        return trimmed;
    }

    private synchronized Object put(String namespace, String key, Object value) {
        drainReferenceQueue();

        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedKey = requireKey(key);

        if (value == null) {
            remove(normalizedNamespace, normalizedKey);
            return null;
        }

        long now = System.currentTimeMillis();
        StoreKey storeKey = new StoreKey(normalizedNamespace, normalizedKey);
        long id = idGenerator.incrementAndGet();
        Entry entry = Entry.create(id, storeKey, value, now, referenceQueue);
        entries.put(storeKey, entry);
        evictIfNecessary();
        return value;
    }

    private synchronized Object get(String namespace, String key) {
        drainReferenceQueue();

        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedKey = requireKey(key);

        StoreKey storeKey = new StoreKey(normalizedNamespace, normalizedKey);
        Entry entry = entries.get(storeKey);
        if (entry == null) {
            return null;
        }

        Object value = entry.reference.get();
        if (value == null) {
            entries.remove(storeKey);
            return null;
        }
        entry.lastAccessTime = System.currentTimeMillis();
        return value;
    }

    private synchronized Object remove(String namespace, String key) {
        drainReferenceQueue();

        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedKey = requireKey(key);

        StoreKey storeKey = new StoreKey(normalizedNamespace, normalizedKey);
        Entry entry = entries.remove(storeKey);
        if (entry == null) {
            return null;
        }
        return entry.reference.get();
    }

    private synchronized void clearNamespace(String namespace) {
        drainReferenceQueue();

        String normalizedNamespace = normalizeNamespace(namespace);
        Iterator<Map.Entry<StoreKey, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<StoreKey, Entry> mapEntry = it.next();
            if (normalizedNamespace.equals(mapEntry.getKey().namespace)) {
                it.remove();
            }
        }
    }

    private synchronized void clearAll() {
        drainReferenceQueue();
        entries.clear();
    }

    private synchronized List<Map<String, Object>> list(String namespace) {
        drainReferenceQueue();

        String normalizedNamespace = normalizeNamespace(namespace);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<StoreKey, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<StoreKey, Entry> mapEntry = it.next();
            StoreKey storeKey = mapEntry.getKey();
            if (!normalizedNamespace.equals(storeKey.namespace)) {
                continue;
            }

            Entry entry = mapEntry.getValue();
            Object value = entry.reference.get();
            if (value == null) {
                it.remove();
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("namespace", storeKey.namespace);
            item.put("name", storeKey.key);
            item.put("class", entry.className);
            item.put("identityHash", entry.identityHashHex);
            item.put("classLoader", entry.classLoader);
            item.put("createTime", entry.createTime);
            item.put("lastAccessTime", entry.lastAccessTime);
            item.put("ageMillis", now - entry.createTime);
            item.put("idleMillis", now - entry.lastAccessTime);
            result.add(item);
        }

        return result;
    }

    private synchronized List<String> namespaces() {
        drainReferenceQueue();

        Set<String> namespaces = new LinkedHashSet<String>();
        Iterator<Map.Entry<StoreKey, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<StoreKey, Entry> mapEntry = it.next();
            Entry entry = mapEntry.getValue();
            if (entry.reference.get() == null) {
                it.remove();
                continue;
            }
            namespaces.add(mapEntry.getKey().namespace);
        }
        List<String> result = new ArrayList<String>(namespaces);
        Collections.sort(result);
        return result;
    }

    private void evictIfNecessary() {
        while (entries.size() > maxEntries) {
            Iterator<Map.Entry<StoreKey, Entry>> it = entries.entrySet().iterator();
            if (!it.hasNext()) {
                return;
            }
            it.next();
            it.remove();
        }
    }

    public static final class Ref {
        private final ObjectRefStore store;
        private final String namespace;

        private Ref(ObjectRefStore store, String namespace) {
            this.store = store;
            this.namespace = namespace;
        }

        public Ref ns(String namespace) {
            return new Ref(store, normalizeNamespace(namespace));
        }

        public String namespace() {
            return namespace;
        }

        public Object put(String key, Object value) {
            return store.put(namespace, key, value);
        }

        public Object get(String key) {
            return store.get(namespace, key);
        }

        public Object remove(String key) {
            return store.remove(namespace, key);
        }

        public void clear() {
            store.clearNamespace(namespace);
        }

        public void clearAll() {
            store.clearAll();
        }

        public List<Map<String, Object>> ls() {
            return store.list(namespace);
        }

        public List<String> namespaces() {
            return store.namespaces();
        }
    }

    private static final class StoreKey {
        private final String namespace;
        private final String key;
        private final int hash;

        private StoreKey(String namespace, String key) {
            this.namespace = namespace;
            this.key = key;
            this.hash = 31 * namespace.hashCode() + key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StoreKey)) {
                return false;
            }
            StoreKey other = (StoreKey) obj;
            return namespace.equals(other.namespace) && key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class Entry {
        private final long id;
        private final KeyedWeakReference reference;
        private final String className;
        private final String classLoader;
        private final String identityHashHex;
        private final long createTime;
        private volatile long lastAccessTime;

        private Entry(long id, KeyedWeakReference reference, String className, String classLoader, String identityHashHex,
                long createTime, long lastAccessTime) {
            this.id = id;
            this.reference = reference;
            this.className = className;
            this.classLoader = classLoader;
            this.identityHashHex = identityHashHex;
            this.createTime = createTime;
            this.lastAccessTime = lastAccessTime;
        }

        private static Entry create(long id, StoreKey key, Object value, long now, ReferenceQueue<Object> queue) {
            Class<?> clazz = value.getClass();
            String className = clazz.getName();

            ClassLoader loader = clazz.getClassLoader();
            String classLoader = loader == null ? "bootstrap"
                    : loader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(loader));

            String identityHashHex = Integer.toHexString(System.identityHashCode(value));
            KeyedWeakReference reference = new KeyedWeakReference(value, queue, key, id);
            return new Entry(id, reference, className, classLoader, identityHashHex, now, now);
        }
    }

    private static final class KeyedWeakReference extends WeakReference<Object> {
        private final StoreKey key;
        private final long id;

        private KeyedWeakReference(Object referent, ReferenceQueue<Object> q, StoreKey key, long id) {
            super(referent, q);
            this.key = key;
            this.id = id;
        }
    }
}

