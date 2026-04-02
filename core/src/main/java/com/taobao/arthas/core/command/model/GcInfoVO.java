package com.taobao.arthas.core.command.model;

/**
 * GC信息视图对象（Value Object）类
 *
 * <p>该类用于封装垃圾回收器（Garbage Collector）的统计信息。
 * 主要用于dashboard命令中展示JVM的GC运行状态。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>记录GC回收器的名称</li>
 *   <li>统计GC发生的次数</li>
 *   <li>统计GC消耗的总时间</li>
 *   <li>为监控和性能分析提供数据支持</li>
 * </ul>
 *
 * @author gongdewei 2020/4/23
 */
public class GcInfoVO {
    /**
     * GC回收器名称
     * 表示JVM中使用的垃圾回收器类型，如"G1 Young Generation"、"G1 Old Generation"等
     */
    private String name;

    /**
     * GC回收次数
     * 表示自JVM启动以来，该垃圾回收器执行的垃圾收集总次数
     */
    private long collectionCount;

    /**
     * GC回收总耗时
     * 表示该垃圾回收器执行所有垃圾收集累计消耗的时间，单位为毫秒
     */
    private long collectionTime;

    /**
     * 构造函数
     *
     * @param name GC回收器名称
     * @param collectionCount GC回收次数
     * @param collectionTime GC回收总耗时（毫秒）
     */
    public GcInfoVO(String name, long collectionCount, long collectionTime) {
        this.name = name;
        this.collectionCount = collectionCount;
        this.collectionTime = collectionTime;
    }

    /**
     * 获取GC回收器名称
     *
     * @return GC回收器的名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 设置GC回收器名称
     *
     * @param name 要设置的GC回收器名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取GC回收次数
     *
     * @return GC发生的总次数
     */
    public long getCollectionCount() {
        return collectionCount;
    }

    /**
     * 设置GC回收次数
     *
     * @param collectionCount 要设置的GC回收次数
     */
    public void setCollectionCount(long collectionCount) {
        this.collectionCount = collectionCount;
    }

    /**
     * 获取GC回收总耗时
     *
     * @return GC累计消耗的总时间（毫秒）
     */
    public long getCollectionTime() {
        return collectionTime;
    }

    /**
     * 设置GC回收总耗时
     *
     * @param collectionTime 要设置的GC回收总耗时（毫秒）
     */
    public void setCollectionTime(long collectionTime) {
        this.collectionTime = collectionTime;
    }
}
