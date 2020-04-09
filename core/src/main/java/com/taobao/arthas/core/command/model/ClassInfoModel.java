package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.ClassUtils;

/**
 * Class info of SearchClassCommand
 * @author gongdewei 2020/04/08
 */
public class ClassInfoModel extends ResultModel {
    private ClassVO classInfo;
    private Class clazz;
    private boolean withField;
    private boolean detail;
    private Integer expand;

    public ClassInfoModel() {
    }

    public ClassInfoModel(Class clazz, boolean detail, boolean withField, Integer expand) {
        this.clazz = clazz;
        this.detail = detail;
        this.withField = withField;
        this.expand = expand;
    }

    @Override
    public String getType() {
        return "class";
    }

    public ClassVO getClassInfo() {
        if (classInfo == null) {
            synchronized (this) {
                classInfo = ClassUtils.createClassInfo(clazz, detail, withField);
            }
        }
        return classInfo;
    }

    public void setClassInfo(ClassVO classInfo) {
        this.classInfo = classInfo;
    }

    public Class clazz() {
        return clazz;
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
