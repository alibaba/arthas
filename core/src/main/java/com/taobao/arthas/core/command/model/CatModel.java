package com.taobao.arthas.core.command.model;

/**
 * Result model for CatCommand
 * @author gongdewei 2020/5/11
 */
public class CatModel extends ResultModel implements Countable {

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

    @Override
    public int size() {
        if (content != null) {
            //粗略计算行数作为item size
            return content.length()/100 + 1;
        }
        return 0;
    }
}
