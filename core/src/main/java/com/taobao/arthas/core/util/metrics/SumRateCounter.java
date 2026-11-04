package com.taobao.arthas.core.util.metrics;

/**
 * <pre>
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

    RateCounter rateCounter;

    Long previous = null;

    public SumRateCounter() {
        rateCounter = new RateCounter();
    }

    public SumRateCounter(int size) {
        rateCounter = new RateCounter(size);
    }

    public int size() {
        return rateCounter.size();
    }

    public void update(long value) {
        if (previous == null) {
            previous = value;
            return;
        }
        rateCounter.update(value - previous);
        previous = value;
    }

    public double rate() {
        return rateCounter.rate();
    }

}
