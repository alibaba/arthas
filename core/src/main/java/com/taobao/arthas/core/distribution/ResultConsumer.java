package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.model.ResultModel;

import java.util.List;

/**
 * 命令结果消费者接口
 *
 * 该接口定义了命令执行结果的消费者行为，负责接收、存储和管理命令的执行结果。
 * 消费者采用队列模式，可以追加阶段性结果，并批量拉取结果进行处理。
 * 该接口还提供了健康检查、状态管理和资源清理等生命周期管理功能。
 *
 * 典型使用场景：
 * 1. 命令执行过程中，通过 appendResult() 方法逐步追加阶段性结果
 * 2. 消费者通过 pollResults() 方法批量拉取结果进行处理或传输
 * 3. 通过 isHealthy() 监控消费者健康状态
 * 4. 使用完毕后通过 close() 释放资源
 *
 * @author gongdewei 2020-03-26
 */
public interface ResultConsumer {

    /**
     * 将阶段性结果追加到队列中
     *
     * 该方法用于将命令执行的一个阶段性结果添加到消费者内部的队列中。
     * 返回值表示结果是否被成功接受和处理。
     *
     * @param result 命令的一个阶段性执行结果，不能为 null
     * @return true 表示分发成功，结果已被接受；false 表示数据被丢弃（可能由于队列已满或消费者已关闭）
     */
    boolean appendResult(ResultModel result);

    /**
     * 从队列头部获取并移除一批结果
     *
     * 该方法批量获取并移除队列头部的结果集合。
     * 如果队列为空，返回空列表或 null（取决于具体实现）。
     * 该方法是阻塞或非阻塞取决于具体实现类。
     *
     * @return 一批命令执行结果的列表，可能为空列表但不为 null
     */
    List<ResultModel> pollResults();

    /**
     * 获取消费者最后一次访问的时间戳
     *
     * 该时间戳用于监控消费者的活跃状态，可用于判断消费者是否闲置超时。
     * 每次调用 appendResult() 或 pollResults() 等方法时，通常会更新此时间戳。
     *
     * @return 最后一次访问时间的毫秒时间戳（System.currentTimeMillis() 格式）
     */
    long getLastAccessTime();

    /**
     * 关闭消费者并释放相关资源
     *
     * 调用该方法后，消费者将停止接受新的结果，并释放占用的系统资源。
     * 关闭后的消费者不应再被使用。实现类应确保该方法是幂等的，多次调用不会产生副作用。
     */
    void close();

    /**
     * 判断消费者是否已关闭
     *
     * 该方法用于检查消费者的当前状态，判断其是否已停止服务。
     * 已关闭的消费者不应再接受新的结果或提供结果查询服务。
     *
     * @return true 表示消费者已关闭；false 表示消费者仍在运行中
     */
    boolean isClosed();

    /**
     * 判断消费者是否正在轮询结果
     *
     * 该方法用于检查是否有其他线程正在通过 pollResults() 方法获取结果。
     * 此状态信息可用于并发控制和负载均衡。
     *
     * @return true 表示有活跃的轮询操作；false 表示当前没有轮询操作
     */
    boolean isPolling();

    /**
     * 获取消费者的唯一标识符
     *
     * 该 ID 用于在分布式环境中唯一标识一个消费者实例，
     * 可用于日志跟踪、监控管理和集群协调等场景。
     *
     * @return 消费者的唯一标识字符串，不应为 null
     */
    String getConsumerId();

    /**
     * 设置消费者的唯一标识符
     *
     * 该方法用于为消费者实例分配一个全局唯一的标识符。
     * 通常在消费者创建后立即调用，且不应频繁更改。
     *
     * @param consumerId 要设置的唯一标识符，不能为 null
     */
    void setConsumerId(String consumerId);

    /**
     * 获取消费者的健康状态
     *
     * 该方法用于检查消费者是否处于正常工作状态。
     * 健康检查可能包括：队列是否溢满、处理速度是否正常、是否有异常发生等。
     * 不健康的消费者可能需要重启或从负载均衡中移除。
     *
     * @return true 表示消费者健康；false 表示消费者存在健康问题
     */
    boolean isHealthy();
}
