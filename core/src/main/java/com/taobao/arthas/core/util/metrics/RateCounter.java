package com.taobao.arthas.core.util.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 速率计数器
 * 用于统计平均速率，例如统计5秒内的平均速率
 * <pre>
 * 示例：5秒的数据是：234, 345, 124, 366, 235
 * 则速率是 (234 + 345 + 124 + 366 + 235) / 5 = 260
 * </pre>
 *
 * 该类使用原子操作保证线程安全，并使用采样算法来处理超过容量的数据
 *
 * @author hengyunabc 2015年12月18日 下午3:40:19
 *
 */
public class RateCounter {
    /**
     * long类型的位数（减去符号位）
     * 用于生成随机数时的掩码计算
     */
    private static final int BITS_PER_LONG = 63;

    /**
     * 默认的采样大小
     * 默认保留最近5个样本值
     */
    public static final int DEFAULT_SIZE = 5;

    /**
     * 计数器
     * 记录总共更新了多少次（包括被采样的次数）
     */
    private final AtomicLong count = new AtomicLong();

    /**
     * 存储采样值的数组
     * 使用原子数组保证线程安全
     * 当更新次数超过数组长度时，使用随机采样策略替换旧值
     */
    private final AtomicLongArray values;

    /**
     * 默认构造函数
     * 使用默认大小（5）创建速率计数器
     */
    public RateCounter() {
        this(DEFAULT_SIZE);
    }

    /**
     * 指定大小的构造函数
     *
     * @param size 采样数组的大小
     */
    public RateCounter(int size) {
        // 创建指定大小的原子长整型数组
        this.values = new AtomicLongArray(size);
        // 初始化数组元素为0
        for (int i = 0; i < values.length(); i++) {
            values.set(i, 0);
        }
        // 初始化计数器为0
        count.set(0);
    }

    /**
     * 获取当前有效的样本数量
     * 当更新次数小于数组长度时，返回实际更新次数
     * 当更新次数超过数组长度时，返回数组长度
     *
     * @return 有效的样本数量
     */
    public int size() {
        // 获取当前的总更新次数
        final long c = count.get();
        // 如果更新次数超过数组长度，返回数组长度
        if (c > values.length()) {
            return values.length();
        }
        // 否则返回实际更新次数
        return (int) c;
    }

    /**
     * 更新计数器，添加一个新的样本值
     * 当样本数量未达到数组容量时，直接添加
     * 当样本数量超过数组容量时，使用随机采样策略替换数组中的某个值
     *
     * @param value 要添加的样本值
     */
    public void update(long value) {
        // 原子性地递增计数器，获取新的计数值
        final long c = count.incrementAndGet();
        // 如果计数值小于等于数组长度，直接按顺序存储
        if (c <= values.length()) {
            values.set((int) c - 1, value);
        } else {
            // 如果超出容量，使用随机采样策略
            // 生成一个0到c-1之间的随机数
            final long r = nextLong(c);
            // 只有当随机数在数组索引范围内时才替换
            if (r < values.length()) {
                values.set((int) r, value);
            }
        }
    }

    /**
     * 计算当前的平均速率
     * 将所有有效样本值相加，然后除以样本数量
     *
     * @return 平均速率
     */
    public double rate() {
        // 获取当前的总更新次数
        long c = count.get();
        int countLength = 0;
        long sum = 0;
        // 确定实际的样本数量
        if (c > values.length()) {
            // 如果超出容量，样本数量为数组长度
            countLength = values.length();
        } else {
            // 否则为实际更新次数
            countLength = (int) c;
        }

        // 累加所有有效样本的值
        for (int i = 0; i < countLength; ++i) {
            sum += values.get(i);
        }

        // 返回平均值（总和除以样本数量）
        return sum / (double) countLength;
    }

    /**
     * 生成一个在[0, n-1]范围内的均匀分布的伪随机long值
     * 该方法从{@link java.util.Random#nextInt()}借鉴而来
     *
     * 使用拒绝采样算法确保随机数的均匀分布，避免模运算偏差
     *
     * @param n 上界（不包含）
     * @return 从范围{@code [0..n)}中随机选择的值
     */
    private static long nextLong(long n) {
        long bits, val;
        do {
            // 从ThreadLocalRandom获取一个随机long值
            // 使用掩码去掉符号位，确保为正数
            bits = ThreadLocalRandom.current().nextLong() & (~(1L << BITS_PER_LONG));
            // 计算模n的值
            val = bits % n;
            // 检查是否有模运算偏差
            // 如果bits - val + (n - 1)溢出（小于0），说明这个值可能有偏差，需要重新采样
        } while (bits - val + (n - 1) < 0L);
        return val;
    }

}
