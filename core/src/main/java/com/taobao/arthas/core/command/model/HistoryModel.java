package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/8
 */
public class HistoryModel extends ResultModel {

    private List<String> history;

    public HistoryModel() {
    }

    public HistoryModel(List<String> history) {
        this.history = history;
    }

    public List<String> getHistory() {
        return history;
    }

    @Override
    public String getType() {
        return "history";
    }
}
