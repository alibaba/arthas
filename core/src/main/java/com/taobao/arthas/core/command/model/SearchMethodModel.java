package com.taobao.arthas.core.command.model;


/**
 * Model of SearchMethodCommand
 * @author gongdewei 2020/4/9
 */
public class SearchMethodModel extends ResultModel {
    private MethodVO methodInfo;
    private boolean detail;

    public SearchMethodModel() {
    }

    public SearchMethodModel(MethodVO methodInfo, boolean detail) {
        this.methodInfo = methodInfo;
        this.detail = detail;
    }

    public MethodVO getMethodInfo() {
        return methodInfo;
    }

    public void setMethodInfo(MethodVO methodInfo) {
        this.methodInfo = methodInfo;
    }

    public boolean isDetail() {
        return detail;
    }

    public void setDetail(boolean detail) {
        this.detail = detail;
    }

    @Override
    public String getType() {
        return "sm";
    }
}
