package com.taobao.arthas.core.command.model;

/**
 * 
 * @author hengyunabc 2021-01-05
 *
 */
public class Base64Model extends ResultModel {

    private String content;

    public Base64Model() {
    }

    public Base64Model(String content) {
        this.content = content;
    }

    @Override
    public String getType() {
        return "base64";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
