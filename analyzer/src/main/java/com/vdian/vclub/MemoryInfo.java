package com.vdian.vclub;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author zhangzicheng
 * @date 2021/01/16
 */
public class MemoryInfo {

    private static final BigDecimal DIVIDER = new BigDecimal(1024);

    /**
     * 类的类型
     */
    private final Class<?> klass;

    /**
     * 当前所有存活对象占用的内存
     */
    private final long sumSize;

    /**
     * 当前所有存活对象占用内存的百分比
     */
    private double percentage;

    private final BigDecimal bigDecimal;

    public MemoryInfo(Class<?> klass, long sumSize) {
        this.klass = klass;
        this.sumSize = sumSize;
        this.bigDecimal = new BigDecimal(sumSize);
    }

    public Class<?> getKlass() {
        return klass;
    }

    public long getSumSize() {
        return sumSize;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public String toString() {
        return "{" + klass.toString() +
                ", sumSize=" + bigDecimal.divide(DIVIDER, 2, RoundingMode.HALF_UP).doubleValue() + " KB" +
                ", percentage=" + percentage + "%}";
    }
}
