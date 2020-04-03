package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.model.ResultModel;

import java.util.List;

/**
 * Command result consumer
 * @author gongdewei 2020-03-26
 */
public interface ResultConsumer {

    /**
     * Append the phased result to queue
     * @param result a phased result of the command
     */
    void appendResult(ResultModel result);

    /**
     * Retrieves and removes a pack of results from the head
     * @return a pack of results
     */
    List<ResultModel> pollResults();

    long getLastAccessTime();

    boolean isPolling();

    String getConsumerId();

    void setConsumerId(String consumerId);
}
