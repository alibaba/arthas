package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * @author gongdewei 2020/4/22
 */
public class JadModel extends ResultModel {
    private ClassVO classInfo;
    private String location;
    private String source;

    //match multiple classes
    private Collection<ClassVO> matchedClasses;

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

    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    public void setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }
}
