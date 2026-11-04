package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/5/11
 */
public class PwdModel extends ResultModel {
    private String workingDir;

    public PwdModel() {
    }

    public PwdModel(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public String getType() {
        return "pwd";
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }
}
