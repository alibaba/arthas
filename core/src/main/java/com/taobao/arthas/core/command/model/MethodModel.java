package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Model of SearchMethodCommand
 * @author gongdewei 2020/4/9
 */
public class MethodModel extends ResultModel {
    private MethodVO methodInfo;
    private boolean detail;

    public MethodModel() {
    }

    public MethodModel(MethodVO methodInfo, boolean detail) {
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
        return "method";
    }
}
