package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/27
 */
public class ProfilerModel extends ResultModel {

    private String action;
    private String actionArg;
    private Object result;
    private String file;
    private String format;

    public ProfilerModel() {
    }

    public ProfilerModel(String action, String actionArg, Object result) {
        this.action = action;
        this.actionArg = actionArg;
        this.result = result;
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
