package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/15
 */
public class OptionsModel extends ResultModel{
    private List<OptionVO> options;

    public OptionsModel() {
    }

    public OptionsModel(List<OptionVO> options) {
        this.options = options;
    }

    @Override
    public String getType() {
        return "option";
    }

    public List<OptionVO> getOptions() {
        return options;
    }

    public void setOptions(List<OptionVO> options) {
        this.options = options;
    }
}
