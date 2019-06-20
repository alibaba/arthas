package com.taobao.arthas.core.util;

/**
 * 简单的调用计时器。TODO 用stack的实现更合理
 * Created by vlinux on 16/6/1.
 * @author hengyunabc 2016-10-31
 */
public class ThreadLocalWatch {

    private final ThreadLocal<Long> timestampRef = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return System.nanoTime();
        }
    };

    public long start() {
        final long timestamp = System.nanoTime();
        timestampRef.set(timestamp);
        return timestamp;
    }

    public long cost() {
        long ct = System.nanoTime();
        do {
            ct = System.nanoTime();
        } while (ct < timestampRef.get());
        return (ct - timestampRef.get());
    }

    public double costInMillis() {
        return (double)cost() / 1000000.0;
    }

    public void clear() {
        timestampRef.remove();
    }

}