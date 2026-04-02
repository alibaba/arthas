package com.taobao.arthas.core.util;

/**
 * 简单的调用计时器
 * 使用ThreadLocal实现线程安全的计时功能
 *
 * @author vlinux 16/6/1.
 * @author hengyunabc 2016-10-31
 */
public class ThreadLocalWatch {

    /**
     * 使用 long[] 做一个固定大小的环形栈，避免把 ArthasClassLoader 加载的对象塞到业务线程的 ThreadLocalMap 里，
     * 从而在 stop/detach 后导致 ArthasClassLoader 无法被 GC 回收。
     *
     * <pre>
     * 约定：
     * - stack[0] 存储当前 pos（0..cap）
     * - stack[1..cap] 存储数据
     * </pre>
     */
    /** 默认栈大小 */
    private static final int DEFAULT_STACK_SIZE = 1024 * 4;
    /** ThreadLocal引用，每个线程维护自己的时间戳数组 */
    private final ThreadLocal<long[]> timestampRef = ThreadLocal.withInitial(() -> new long[DEFAULT_STACK_SIZE + 1]);

    /**
     * 开始计时
     * 将当前时间戳压入栈中
     *
     * @return 当前时间戳（纳秒）
     */
    public long start() {
        final long timestamp = System.nanoTime();
        push(timestampRef.get(), timestamp);
        return timestamp;
    }

    /**
     * 计算耗时（纳秒）
     * 从栈中弹出时间戳并计算与当前时间的差值
     *
     * @return 耗时（纳秒）
     */
    public long cost() {
        return (System.nanoTime() - pop(timestampRef.get()));
    }

    /**
     * 计算耗时（毫秒）
     * 从栈中弹出时间戳并计算与当前时间的差值
     *
     * @return 耗时（毫秒）
     */
    public double costInMillis() {
        return (System.nanoTime() - pop(timestampRef.get())) / 1000000.0;
    }

    /**
     * 将值压入栈中
     *
     * <pre>
     * 一个特殊的栈，为了追求效率，避免扩容。
     * 因为这个栈的push/pop 并不一定成对调用，比如可能push执行了，但是后面的流程被中断了，pop没有被执行。
     * 如果不固定大小，一直增长的话，极端情况下可能导致应用有内存问题。
     * 如果到达容量，pos会重置，循环存储数据。所以使用这个栈在极端情况下统计的数据会不准确，只用于monitor/watch等命令的计时。
     *
     * </pre>
     *
     * @author hengyunabc 2019-11-20
     *
     * @param stack 栈数组
     * @param value 要压入的值
     */
    static void push(long[] stack, long value) {
        int cap = stack.length - 1;
        int pos = (int) stack[0];
        if (pos < cap) {
            pos++;
        } else {
            // 如果栈已满，重置位置到1
            pos = 1;
        }
        stack[pos] = value;
        stack[0] = pos;
    }

    /**
     * 从栈中弹出值
     *
     * @param stack 栈数组
     * @return 弹出的值
     */
    static long pop(long[] stack) {
        int cap = stack.length - 1;
        int pos = (int) stack[0];
        if (pos > 0) {
            // 如果当前位置大于0，弹出当前位置的值
            long value = stack[pos];
            stack[0] = pos - 1;
            return value;
        }

        // 如果当前位置为0，说明栈已经空了，从末尾弹出值（循环使用）
        pos = cap;
        long value = stack[pos];
        stack[0] = pos - 1;
        return value;
    }
}
