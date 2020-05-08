package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.ClassUtils;

/**
 * Class info of SearchClassCommand
 * @author gongdewei 2020/04/08
 */
public class ClassInfoModel extends ResultModel {
    private ClassVO classInfo;
    private boolean withField;
    private boolean detail;
    private Integer expand;

    public ClassInfoModel() {
    }

    public ClassInfoModel(ClassVO classInfo, boolean detail, boolean withField, Integer expand) {
        this.classInfo = classInfo;
        this.detail = detail;
        this.withField = withField;
        this.expand = expand;
    }

    @Override
    public String getType() {
        return "class";
    }

    public ClassVO getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassVO classInfo) {
        this.classInfo = classInfo;
    }

    public boolean detail() {
        return detail;
    }

    public boolean withField() {
        return withField;
    }

    public Integer expand() {
        return expand;
    }

}
