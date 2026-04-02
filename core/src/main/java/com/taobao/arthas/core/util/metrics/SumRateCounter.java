package com.taobao.arthas.core.util.metrics;

/**
 * 总和速率计数器
 *
 * 该类用于统计累积数据的速率。适用于统计单调递增的总量数据的变化速率，
 * 例如累计请求数、累计字节数等。
 *
 * <pre>
 * 工作原理：
 * 统计传入的数据是总数的速率。
 * 比如传入的数据是所有请求的数量，5秒数据为：
 * 267, 457, 635, 894, 1398
 * 则统计的平均速率是：( (457-267) + (635-457) + (894-635) + (1398-894) ) / 4 = 282
 * </pre>
 *
 * @author hengyunabc 2015年12月18日 下午3:40:26
 *
 */
public class SumRateCounter {

    /**
     * 速率计数器
     * 内部委托给 RateCounter 来计算速率平均值
     */
    RateCounter rateCounter;

    /**
     * 上一次的值
     * 用于保存上一次传入的累积值，以便计算增量
     * 初始为 null，表示第一次更新
     */
    Long previous = null;

    /**
     * 默认构造函数
     * 使用默认的速率计数器大小
     */
    public SumRateCounter() {
        rateCounter = new RateCounter();
    }

    /**
     * 指定大小的构造函数
     *
     * @param size 速率计数器的大小，即保存最近几次的增量数据用于计算平均速率
     */
    public SumRateCounter(int size) {
        rateCounter = new RateCounter(size);
    }

    /**
     * 获取速率计数器的大小
     *
     * @return 速率计数器保存的数据点数量
     */
    public int size() {
        return rateCounter.size();
    }

    /**
     * 更新累积值
     *
     * 该方法接收一个累积值，计算与上一次值的增量，并将增量传递给内部的 RateCounter。
     * 第一次调用时会保存当前值但不计算速率。
     *
     * @param value 当前的累积值（单调递增）
     */
    public void update(long value) {
        // 如果是第一次更新，保存当前值并返回
        if (previous == null) {
            previous = value;
            return;
        }
        // 计算增量并更新速率计数器
        rateCounter.update(value - previous);
        // 保存当前值作为下一次的前值
        previous = value;
    }

    /**
     * 获取当前的平均速率
     *
     * @return 基于最近几次增量数据计算的平均速率
     */
    public double rate() {
        return rateCounter.rate();
    }

}
