package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/5/11
 */
public class CatModel extends ResultModel {

    private String file;
    private String content;

    public CatModel() {
    }

    public CatModel(String file, String content) {
        this.file = file;
        this.content = content;
    }

    @Override
    public String getType() {
        return "cat";
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
