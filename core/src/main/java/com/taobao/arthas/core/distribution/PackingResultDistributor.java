package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.result.ExecResult;

import java.util.List;

public interface PackingResultDistributor extends ResultDistributor {

    /**
     * Get results of command
     */
    List<ExecResult> getResults();

}
