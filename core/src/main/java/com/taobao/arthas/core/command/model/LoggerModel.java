package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.Map;

/**
 * Model of logger command
 *
 * @author gongdewei 2020/4/22
 */
public class LoggerModel extends ResultModel {

    private Map<String, Map<String, Object>> loggerInfoMap;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

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

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public LoggerModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public LoggerModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    @Override
    public String getType() {
        return "logger";
    }

}
