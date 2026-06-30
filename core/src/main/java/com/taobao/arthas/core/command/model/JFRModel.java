package com.taobao.arthas.core.command.model;

/**
 * @author xulong 2022/7/25
 */
public class JFRModel extends ResultModel {

    private String jfrOutput = "";

    @Override
    public String getType() {
        return "jfr";
    }

    public String getJfrOutput() {
        return jfrOutput;
    }

    public void setJfrOutput(String jfrOutput) {
        this.jfrOutput += jfrOutput;
    }
}
