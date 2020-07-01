package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/5/11
 */
public class EchoModel extends ResultModel {

    private String content;

    public EchoModel() {
    }

    public EchoModel(String content) {
        this.content = content;
    }

    @Override
    public String getType() {
        return "echo";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
