package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.command.model.ResultModel;

import java.util.List;

/**
 * 打包结果分发器接口
 *
 * 该接口继承自 ResultDistributor，扩展了获取命令执行结果的功能。
 * 实现该接口的类负责将命令执行结果进行打包分发，并提供获取已分发结果的访问方法。
 *
 * @see ResultDistributor
 * @see ResultModel
 */
public interface PackingResultDistributor extends ResultDistributor {

    /**
     * 获取命令的执行结果
     *
     * 该方法返回当前已分发的所有命令执行结果列表。
     * 调用方可以通过此方法获取完整的命令执行结果集合，用于后续处理或展示。
     *
     * @return 命令执行结果的列表，每个 ResultModel 对象代表一个命令的执行结果
     */
    List<ResultModel> getResults();

}
