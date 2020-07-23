package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * Data model of ProfilerCommand
 * @author gongdewei 2020/4/27
 */
public class ProfilerModel extends ResultModel {

    private String action;
    private String actionArg;
    private String executeResult;
    private Collection<String> supportedActions;
    private String outputFile;
    private Long duration;

    public ProfilerModel() {
    }

    public ProfilerModel(Collection<String> supportedActions) {
        this.supportedActions = supportedActions;
    }

    @Override
    public String getType() {
        return "profiler";
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionArg() {
        return actionArg;
    }

    public void setActionArg(String actionArg) {
        this.actionArg = actionArg;
    }

    public Collection<String> getSupportedActions() {
        return supportedActions;
    }

    public void setSupportedActions(Collection<String> supportedActions) {
        this.supportedActions = supportedActions;
    }

    public String getExecuteResult() {
        return executeResult;
    }

    public void setExecuteResult(String executeResult) {
        this.executeResult = executeResult;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
