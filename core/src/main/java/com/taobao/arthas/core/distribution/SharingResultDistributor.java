package com.taobao.arthas.core.distribution;

import java.util.List;

/**
 * 可共享的结果分发器接口
 * <p>
 * 该接口扩展了ResultDistributor，支持多个消费者共享同一个会话的命令执行结果。
 * 与普通分发器不同，共享分发器允许动态地添加和移除消费者，实现了一对多的结果分发模式。
 * </p>
 * <p>
 * 主要应用场景：
 * <ul>
 * <li>多个客户端同时观看同一个命令的执行过程</li>
 * <li>协作式诊断场景，多个开发者共享同一会话</li>
 * <li>实时监控和演示场景</li>
 * </ul>
 * </p>
 */
public interface SharingResultDistributor extends ResultDistributor {

    /**
     * 向共享会话中添加一个消费者
     * <p>
     * 添加消费者后，该消费者将开始接收后续所有命令执行的结果。
     * 消费者可以随时加入会话，不会影响已经分发的结果。
     * </p>
     *
     * @param consumer 要添加的消费者对象，不能为null
     */
    void addConsumer(ResultConsumer consumer);

    /**
     * 从共享会话中移除一个消费者
     * <p>
     * 移除消费者后，该消费者将不再接收任何后续的命令执行结果。
     * 此操作不会影响其他消费者的正常接收。
     * </p>
     *
     * @param consumer 要移除的消费者对象，如果消费者不存在则不执行任何操作
     */
    void removeConsumer(ResultConsumer consumer);

    /**
     * 获取当前会话中的所有消费者列表
     * <p>
     * 返回的列表是该会话当前所有活跃消费者的快照。
     * 注意：此列表的修改不会影响实际的消费者集合。
     * </p>
     *
     * @return 所有消费者的列表，如果没有消费者则返回空列表
     */
    List<ResultConsumer> getConsumers();

    /**
     * 根据消费者ID获取对应的消费者对象
     * <p>
     * 用于快速定位和访问特定的消费者实例。
     * </p>
     *
     * @param consumerId 消费者的唯一标识符
     * @return 对应的消费者对象，如果不存在则返回null
     */
    ResultConsumer getConsumer(String consumerId);
}
