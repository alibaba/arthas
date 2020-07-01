package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.shell.term.impl.http.api.ApiState;

/**
 * Command async exec process result, not the command exec result
 * @author gongdewei 2020/4/2
 */
public class CommandRequestModel extends ResultModel {

    private ApiState state;
    private String command;
    private String message;

    public CommandRequestModel(String command, ApiState state) {
        this.command = command;
        this.state = state;
    }

    public CommandRequestModel(String command, ApiState state, String message) {
        this.state = state;
        this.command = command;
        this.message = message;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public ApiState getState() {
        return state;
    }

    public void setState(ApiState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getType() {
        return "command";
    }
}
