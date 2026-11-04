package com.taobao.arthas.core.distribution;

/**
 * 命令结果分发器选项
 * @author gongdewei 2020/5/18
 */
public class DistributorOptions {

    /**
     * ResultConsumer的结果队列长度，用于控制内存缓存的命令结果数据量
     */
    public static int resultQueueSize = 50;

}
