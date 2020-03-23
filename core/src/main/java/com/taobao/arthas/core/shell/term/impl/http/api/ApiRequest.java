package com.taobao.arthas.core.shell.term.impl.http.api;

import java.util.Map;

/**
 * Http Api exception
 * @author gongdewei 2020-03-19
 */
public class ApiRequest {
    private String action;
    private boolean sync;
    private String command;
    private Map<String, Object> options;

    @Override
    public String toString() {
        return "ApiRequest{" +
                "action='" + action + '\'' +
                ", sync=" + sync +
                ", command='" + command + '\'' +
                ", options=" + options +
                '}';
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
