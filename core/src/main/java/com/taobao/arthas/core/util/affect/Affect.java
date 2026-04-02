package com.taobao.arthas.core.util.affect;

import static java.lang.System.currentTimeMillis;

/**
 * 影响反馈类
 * 用于跟踪和记录操作的性能指标，主要计算操作的耗时
 * Created by vlinux on 15/5/21.
 * @author diecui1202 on 2017/10/26
 */
public class Affect {

    /**
     * 操作开始时间戳（毫秒）
     * 使用 final 修饰确保时间戳在对象创建后不会被修改
     */
    private final long start = currentTimeMillis();

    /**
     * 计算操作耗时
     * 通过当前时间减去开始时间，获取操作执行的总耗时
     *
     * @return 返回从对象创建到当前时刻的耗时（毫秒）
     */
    public long cost() {
        // 获取当前时间戳，减去开始时间戳，得到耗时
        return currentTimeMillis() - start;
    }

    /**
     * 将影响信息转换为字符串格式
     * 用于日志输出或显示给用户，展示操作耗时信息
     *
     * @return 格式化后的耗时信息字符串
     */
    @Override
    public String toString() {
        // 使用格式化字符串，将耗时值插入到模板中
        return String.format("Affect cost in %s ms.", cost());
    }
}
