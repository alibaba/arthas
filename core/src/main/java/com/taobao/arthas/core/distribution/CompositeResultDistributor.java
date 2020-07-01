package com.taobao.arthas.core.distribution;

/**
 * 复合结果分发器，将消息同时分发给其包含的多个真实分发器
 * @author gongdewei 2020/4/30
 */
public interface CompositeResultDistributor extends ResultDistributor {

    void addDistributor(ResultDistributor distributor);

    void removeDistributor(ResultDistributor distributor);
}
