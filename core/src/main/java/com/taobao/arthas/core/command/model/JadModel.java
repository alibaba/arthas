package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/22
 */
public class JadModel extends ResultModel {
    private ClassVO classInfo;
    private String location;
    private String source;

    @Override
    public String getType() {
        return "jad";
    }

    public JadModel() {
    }

    public ClassVO getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassVO classInfo) {
        this.classInfo = classInfo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
