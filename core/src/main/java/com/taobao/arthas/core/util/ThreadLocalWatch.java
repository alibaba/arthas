package com.taobao.arthas.core.util;

import java.util.Stack;

/**
 * 简单的调用计时器。TODO 用stack的实现更合理
 * Created by vlinux on 16/6/1.
 * @author hengyunabc 2016-10-31
 */
public class ThreadLocalWatch {

    private final ThreadLocal<Stack<Long>> timestampRef = new ThreadLocal<Stack<Long>>() {
        @Override
        protected Stack<Long> initialValue() {
            return new Stack<Long>();
        }
    };

    public long start() {
        final long timestamp = System.nanoTime();
        timestampRef.get().push(timestamp);
        return timestamp;
    }

    public long cost() {
        return (System.nanoTime() - timestampRef.get().pop());
    }

    public double costInMillis() {
        return (System.nanoTime() - timestampRef.get().pop()) / 1000000.0;
    }

    public void clear() {
        timestampRef.remove();
    }

}