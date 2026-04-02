package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.command.monitor200.MonitorData;

import java.util.List;

/**
 * 监控命令的数据模型
 * 用于封装MonitorCommand的执行结果，包含方法执行监控数据的列表
 *
 * @author gongdewei 2020/4/28
 */
public class MonitorModel extends ResultModel {

    /**
     * 监控数据列表
     * 存储所有被监控方法的执行数据，包括执行时间、调用次数等信息
     */
    private List<MonitorData> monitorDataList;

    /**
     * 默认构造函数
     * 创建一个空的监控数据模型实例
     */
    public MonitorModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param monitorDataList 监控数据列表，包含所有被监控方法的执行数据
     */
    public MonitorModel(List<MonitorData> monitorDataList) {
        this.monitorDataList = monitorDataList;
    }

    /**
     * 获取结果模型的类型标识
     * 用于前端或客户端识别返回的数据类型
     *
     * @return 返回"monitor"字符串标识，表示这是监控命令的返回结果
     */
    @Override
    public String getType() {
        return "monitor";
    }

    /**
     * 获取监控数据列表
     *
     * @return 监控数据列表，包含所有被监控方法的执行数据
     */
    public List<MonitorData> getMonitorDataList() {
        return monitorDataList;
    }

    /**
     * 设置监控数据列表
     *
     * @param monitorDataList 要设置的监控数据列表，包含所有被监控方法的执行数据
     */
    public void setMonitorDataList(List<MonitorData> monitorDataList) {
        this.monitorDataList = monitorDataList;
    }
}
