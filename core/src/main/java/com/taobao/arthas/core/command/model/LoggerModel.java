package com.taobao.arthas.core.command.model;

import java.util.Map;

/**
 * Model of logger command
 *
 * @author gongdewei 2020/4/22
 */
public class LoggerModel extends ResultModel {

    private Map<String, Map<String, Object>> loggerInfoMap;

    public LoggerModel() {
    }

    public LoggerModel(Map<String, Map<String, Object>> loggerInfoMap) {
        this.loggerInfoMap = loggerInfoMap;
    }

    public Map<String, Map<String, Object>> getLoggerInfoMap() {
        return loggerInfoMap;
    }

    public void setLoggerInfoMap(Map<String, Map<String, Object>> loggerInfoMap) {
        this.loggerInfoMap = loggerInfoMap;
    }

    @Override
    public String getType() {
        return "logger";
    }

}
