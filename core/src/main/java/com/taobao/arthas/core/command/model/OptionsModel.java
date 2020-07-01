package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/15
 */
public class OptionsModel extends ResultModel{
    private List<OptionVO> options;
    private ChangeResultVO changeResult;

    public OptionsModel() {
    }

    public OptionsModel(List<OptionVO> options) {
        this.options = options;
    }

    public OptionsModel(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }

    @Override
    public String getType() {
        return "options";
    }

    public List<OptionVO> getOptions() {
        return options;
    }

    public void setOptions(List<OptionVO> options) {
        this.options = options;
    }

    public ChangeResultVO getChangeResult() {
        return changeResult;
    }

    public void setChangeResult(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }
}
