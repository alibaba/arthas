package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * history命令的数据模型
 * 用于封装history命令的执行结果，存储用户执行过的命令历史记录
 *
 * @author gongdewei 2020/4/8
 */
public class HistoryModel extends ResultModel {

    /**
     * 命令历史记录列表
     * 存储用户在Arthas控制台中执行过的命令，按执行顺序排列
     */
    private List<String> history;

    /**
     * 默认构造函数
     * 创建一个空的HistoryModel实例
     */
    public HistoryModel() {
    }

    /**
     * 构造函数 - 用于创建包含命令历史的模型
     *
     * @param history 命令历史记录列表
     */
    public HistoryModel(List<String> history) {
        this.history = history;
    }

    /**
     * 获取命令历史记录列表
     *
     * @return 命令历史记录列表
     */
    public List<String> getHistory() {
        return history;
    }

    /**
     * 获取模型类型
     * 用于标识这是一个history命令的结果模型
     *
     * @return 模型类型标识符 "history"
     */
    @Override
    public String getType() {
        return "history";
    }
}
