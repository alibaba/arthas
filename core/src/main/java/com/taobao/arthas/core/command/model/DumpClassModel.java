package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/21
 */
public class DumpClassModel extends ResultModel {

    private List<ClassVO> classFiles;

    public DumpClassModel() {
    }

    public DumpClassModel(List<ClassVO> classFiles) {
        this.classFiles = classFiles;
    }

    public List<ClassVO> getClassFiles() {
        return classFiles;
    }

    public void setClassFiles(List<ClassVO> classFiles) {
        this.classFiles = classFiles;
    }

    @Override
    public String getType() {
        return "dump";
    }

}
