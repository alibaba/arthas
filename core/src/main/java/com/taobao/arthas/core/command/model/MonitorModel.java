package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.command.monitor200.MonitorData;

import java.util.List;

/**
 * Data model of MonitorCommand
 * @author gongdewei 2020/4/28
 */
public class MonitorModel extends ResultModel {

    private List<MonitorData> monitorDataList;

    public MonitorModel() {
    }

    public MonitorModel(List<MonitorData> monitorDataList) {
        this.monitorDataList = monitorDataList;
    }

    @Override
    public String getType() {
        return "monitor";
    }

    public List<MonitorData> getMonitorDataList() {
        return monitorDataList;
    }

    public void setMonitorDataList(List<MonitorData> monitorDataList) {
        this.monitorDataList = monitorDataList;
    }
}
