package com.taobao.arthas.core.command.model;

public class VersionModel extends ResultModel {

    private String version;

    @Override
    public String getType() {
        return "version";
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
