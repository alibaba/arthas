package com.taobao.arthas.core.distribution;

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

}
