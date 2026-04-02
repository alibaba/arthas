package com.taobao.arthas.core.shell.history;

import java.util.List;

/**
 * 历史记录管理器接口
 *
 * 用于管理Arthas命令行的历史记录，包括添加、获取、保存、加载和清空历史记录等功能
 *
 * @author gongdewei 2020/4/8
 */
public interface HistoryManager {

    /**
     * 添加一条命令行到历史记录
     *
     * @param commandLine 要添加的命令行内容
     */
    void addHistory(String commandLine);

    /**
     * 获取所有的历史记录
     *
     * @return 历史命令行列表，按时间顺序排列
     */
    List<String> getHistory();

    /**
     * 设置历史记录
     *
     * @param history 要设置的历史命令行列表
     */
    void setHistory(List<String> history);

    /**
     * 保存历史记录到持久化存储
     *
     * 通常将历史记录保存到文件中，以便下次启动时可以恢复
     */
    void saveHistory();

    /**
     * 从持久化存储加载历史记录
     *
     * 通常从文件中读取之前保存的历史记录
     */
    void loadHistory();

    /**
     * 清空所有历史记录
     *
     * 同时会清空内存中的历史记录和持久化存储中的记录
     */
    void clearHistory();
}
