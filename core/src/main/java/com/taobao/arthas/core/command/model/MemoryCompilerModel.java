package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/20
 */
public class MemoryCompilerModel extends ResultModel {

    private List<String> files;

    public MemoryCompilerModel() {
    }

    public MemoryCompilerModel(List<String> files) {
        this.files = files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public List<String> getFiles() {
        return files;
    }

    @Override
    public String getType() {
        return "mc";
    }

}
