package com.taobao.arthas.core.distribution;

import java.util.List;

public interface SharingResultDistributor extends ResultDistributor {

    /**
     * Add consumer to sharing session
     * @param consumer
     */
    void addConsumer(ResultConsumer consumer);

    /**
     * Remove consumer from sharing session
     * @param consumer
     */
    void removeConsumer(ResultConsumer consumer);

    /**
     * Get all consumers of session
     * @return
     */
    List<ResultConsumer> getConsumers();

    /**
     * Get consumer by id
     * @param consumerId
     * @return
     */
    ResultConsumer getConsumer(String consumerId);
}
