package com.taobao.arthas.core.command.model;

import com.sun.management.VMOption;

import java.util.List;

/**
 * @author gongdewei 2020/4/15
 */
public class VMOptionModel extends ResultModel {

    private List<VMOption> vmOptions;

    public VMOptionModel() {
    }

    public VMOptionModel(List<VMOption> vmOptions) {
        this.vmOptions = vmOptions;
    }

    @Override
    public String getType() {
        return "vmoption";
    }

    public List<VMOption> getVmOptions() {
        return vmOptions;
    }

    public void setVmOptions(List<VMOption> vmOptions) {
        this.vmOptions = vmOptions;
    }
}
