package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.model.ResultModel;

/**
 * 命令结果分发器接口
 * <p>
 * 该接口定义了将命令执行结果分发给同一会话中所有消费者的基本行为。
 * 当命令执行过程中产生阶段性结果时，通过此接口将结果发送给所有订阅了该会话的消费者。
 * </p>
 * <p>
 * 典型的使用场景包括：
 * <ul>
 * <li>实时将命令执行的中间结果推送给前端界面</li>
 * <li>将最终结果分发给多个订阅者</li>
 * <li>管理会话的生命周期和资源释放</li>
 * </ul>
 * </p>
 *
 * @author gongdewei 2020-03-26
 */
public interface ResultDistributor {

    /**
     * 将命令的阶段性结果追加到分发队列中
     * <p>
     * 此方法用于将命令执行过程中产生的结果（可能是中间结果或最终结果）
     * 添加到分发队列，以便后续分发给所有订阅该会话的消费者。
     * </p>
     *
     * @param result 命令的阶段性结果对象，包含命令执行的输出数据
     */
    void appendResult(ResultModel result);

    /**
     * 关闭结果分发器并释放相关资源
     * <p>
     * 当会话结束时，应该调用此方法来：
     * <ul>
     * <li>停止接收新的结果</li>
     * <li>清空队列中未分发的结果</li>
     * <li>释放与分发器相关的所有系统资源</li>
     * <li>通知所有消费者会话已结束</li>
     * </ul>
     * </p>
     */
    void close();
}
