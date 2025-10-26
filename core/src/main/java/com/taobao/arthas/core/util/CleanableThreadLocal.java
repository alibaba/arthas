package com.taobao.arthas.core.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A ThreadLocal-like utility backed by a ConcurrentHashMap that supports
 * cleaning up all thread-bound values at once via {@link #cleanUp()}.
 *
 * This is a simplified alternative to JDK's {@link ThreadLocal} with an
 * additional ability to clear all stored values, not just the current thread's.
 *
 * @param <T> the type of the value
 */
public class CleanableThreadLocal<T> {

    private final ConcurrentMap<Thread, T> store = new ConcurrentHashMap<Thread, T>();

    /**
     * Returns the current thread's value of this thread-local variable. If the
     * value is absent, it will be initialized via {@link #initialValue()} and
     * stored for the current thread.
     */
    public T get() {
        Thread t = Thread.currentThread();
        T val = store.get(t);
        if (val == null) {
            val = initialValue();
            if (val != null) {
                store.put(t, val);
            }
        }
        return val;
    }

    /**
     * Sets the current thread's value for this variable.
     */
    public void set(T value) {
        if (value == null) {
            remove();
        } else {
            store.put(Thread.currentThread(), value);
        }
    }

    /**
     * Removes the current thread's value for this variable.
     */
    public void remove() {
        store.remove(Thread.currentThread());
    }

    /**
     * Provides the initial value for this variable for the current thread.
     * Subclasses may override to customize initialization.
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Clears all thread-bound values. Unlike {@link #remove()} which only
     * affects the current thread, this method removes values for all threads.
     */
    public void cleanUp() {
        store.clear();
    }
}

