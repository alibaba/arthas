package com.taobao.arthas.core.command.result;

public class VersionResult extends ExecResult {

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
