package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.CompositeResultDistributor;
import com.taobao.arthas.core.distribution.ResultDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 复合结果分发器实现类
 * <p>
 * 该类实现了复合结果分发器接口，将消息同时分发给其包含的所有真实分发器。
 * 它是一个组合模式的实现，可以管理多个分发器，并将结果分发给所有注册的分发器。
 * 使用线程安全的List来存储分发器，确保多线程环境下的安全性。
 *
 * @author gongdewei 2020/4/30
 */
public class CompositeResultDistributorImpl implements CompositeResultDistributor {

    /**
     * 分发器列表
     * <p>
     * 使用Collections.synchronizedList包装ArrayList，确保在多线程环境下对分发器列表的操作是线程安全的。
     * 存储所有注册的结果分发器，当有新结果到达时，会遍历此列表将结果分发给所有分发器。
     */
    private List<ResultDistributor> distributors = Collections.synchronizedList(new ArrayList<ResultDistributor>());

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的复合结果分发器实例，不包含任何子分发器。
     */
    public CompositeResultDistributorImpl() {
    }

    /**
     * 带参数的构造函数
     * <p>
     * 创建一个复合结果分发器实例，并自动将传入的所有分发器注册到复合分发器中。
     * 使用可变参数，允许传入任意数量的分发器。
     *
     * @param distributors 要注册的零个或多个结果分发器
     */
    public CompositeResultDistributorImpl(ResultDistributor ... distributors) {
        // 遍历传入的所有分发器，逐个添加到复合分发器中
        for (ResultDistributor distributor : distributors) {
            this.addDistributor(distributor);
        }
    }

    /**
     * 添加分发器到复合分发器中
     * <p>
     * 将指定的结果分发器添加到分发器列表中。添加后，新的结果将被分发到该分发器。
     * 由于使用了synchronizedList，此操作是线程安全的。
     *
     * @param distributor 要添加的结果分发器，不能为null
     */
    @Override
    public void addDistributor(ResultDistributor distributor) {
        distributors.add(distributor);
    }

    /**
     * 从复合分发器中移除指定的分发器
     * <p>
     * 将指定的结果分发器从分发器列表中移除。移除后，新的结果将不再分发到该分发器。
     * 由于使用了synchronizedList，此操作是线程安全的。
     *
     * @param distributor 要移除的结果分发器
     */
    @Override
    public void removeDistributor(ResultDistributor distributor) {
        distributors.remove(distributor);
    }

    /**
     * 将结果追加到所有分发器
     * <p>
     * 遍历所有已注册的分发器，将结果追加到每个分发器中。
     * 这样可以确保同一个结果被分发到所有注册的输出目标。
     * 如果某个分发器处理失败，不会影响其他分发器的处理。
     *
     * @param result 要追加的结果模型对象
     */
    @Override
    public void appendResult(ResultModel result) {
        // 遍历所有注册的分发器，将结果分发给每一个
        for (ResultDistributor distributor : distributors) {
            distributor.appendResult(result);
        }
    }

    /**
     * 关闭所有分发器
     * <p>
     * 遍历所有已注册的分发器，依次关闭每一个分发器。
     * 关闭操作通常包括释放资源、刷新缓冲区等清理工作。
     * 即使某个分发器关闭失败，也会继续关闭其他分发器。
     */
    @Override
    public void close() {
        // 遍历所有注册的分发器，依次关闭每一个
        for (ResultDistributor distributor : distributors) {
            distributor.close();
        }
    }
}
