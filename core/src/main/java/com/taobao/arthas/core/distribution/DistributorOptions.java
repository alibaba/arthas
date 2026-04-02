package com.taobao.arthas.core.distribution;

/**
 * 命令结果分发器选项配置类
 * <p>
 * 该类用于配置命令结果分发器的各种选项参数，主要用于控制命令结果在内存中的缓存行为。
 * 通过调整这些参数，可以平衡内存使用和命令结果的保存容量。
 * </p>
 *
 * @author gongdewei 2020/5/18
 */
public class DistributorOptions {

    /**
     * ResultConsumer的结果队列长度，用于控制内存缓存的命令结果数据量
     * <p>
     * 该参数定义了结果队列的最大容量，当队列满时，新的命令结果可能会被丢弃或采用其他策略处理。
     * 默认值为50，表示在内存中最多缓存50条命令结果。
     * </p>
     * <p>
     * 注意：此值为静态可变参数，可以在运行时动态调整以适应不同的使用场景。
     * </p>
     */
    public static int resultQueueSize = 50;

}
