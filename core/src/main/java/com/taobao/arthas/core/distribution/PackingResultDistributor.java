package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.model.ResultModel;

import java.util.List;

public interface PackingResultDistributor extends ResultDistributor {

    /**
     * Get results of command
     */
    List<ResultModel> getResults();

}
