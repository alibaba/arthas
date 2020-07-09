package com.taobao.arthas.core.command.model;


import java.util.List;

/**
 * Class info of SearchClassCommand
 * @author gongdewei 2020/04/08
 */
public class SearchClassModel extends ResultModel {
    private ClassDetailVO classInfo;
    private boolean withField;
    private boolean detail;
    private Integer expand;
    private List<String> classNames;
    private int segment;

    public SearchClassModel() {
    }

    public SearchClassModel(ClassDetailVO classInfo, boolean detail, boolean withField, Integer expand) {
        this.classInfo = classInfo;
        this.detail = detail;
        this.withField = withField;
        this.expand = expand;
    }

    public SearchClassModel(List<String> classNames, int segment) {
        this.classNames = classNames;
        this.segment = segment;
    }

    @Override
    public String getType() {
        return "sc";
    }

    public ClassDetailVO getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassDetailVO classInfo) {
        this.classInfo = classInfo;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
    }

    public int getSegment() {
        return segment;
    }

    public void setSegment(int segment) {
        this.segment = segment;
    }

    public boolean isDetail() {
        return detail;
    }

    public boolean withField() {
        return withField;
    }

    public Integer expand() {
        return expand;
    }

}
