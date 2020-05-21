package com.taobao.arthas.core.command.model;

/**
 * Input status for webui
 * @author gongdewei 2020/4/14
 */
public class InputStatusModel extends ResultModel {

    private InputStatus inputStatus;

    public InputStatusModel(InputStatus inputStatus) {
        this.inputStatus = inputStatus;
    }

    public InputStatus getInputStatus() {
        return inputStatus;
    }

    public void setInputStatus(InputStatus inputStatus) {
        this.inputStatus = inputStatus;
    }

    @Override
    public String getType() {
        return "input_status";
    }

}
