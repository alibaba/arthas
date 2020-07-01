package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.model.ResultModel;

/**
 * Command result distributor, sending results to consumers who joins in the same session.
 * @author gongdewei 2020-03-26
 */
public interface ResultDistributor {

    /**
     * Append the phased result to queue
     * @param result a phased result of the command
     */
    void appendResult(ResultModel result);

    /**
     * Close result distribtor, release resources
     */
    void close();
}
