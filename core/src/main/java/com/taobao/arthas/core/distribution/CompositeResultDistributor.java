package com.taobao.arthas.core.distribution;

/**
 * 复合结果分发器接口
 * 将消息同时分发给其包含的多个真实分发器
 * 实现了组合模式，允许将多个分发器组合成一个统一的结构
 *
 * 典型使用场景：
 * - 需要将同一个命令执行结果同时发送给多个不同的接收者
 * - 需要动态地添加或移除结果接收者
 * - 需要对结果进行多路分发处理
 *
 * @author gongdewei 2020/4/30
 */
public interface CompositeResultDistributor extends ResultDistributor {

    /**
     * 添加一个结果分发器
     * 添加后，所有后续的结果都会被分发到这个新加的分发器
     *
     * @param distributor 要添加的结果分发器，不能为null
     */
    void addDistributor(ResultDistributor distributor);

    /**
     * 移除一个结果分发器
     * 移除后，该分发器将不再接收任何结果
     *
     * @param distributor 要移除的结果分发器，如果不存在则忽略
     */
    void removeDistributor(ResultDistributor distributor);
}
