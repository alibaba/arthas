package com.taobao.arthas.core.command.model;

/**
 * Model of `heapdump` command
 * @author gongdewei 2020/4/24
 */
public class HeapDumpModel extends ResultModel {

    private String dumpFile;

    private boolean live;

    public HeapDumpModel() {
    }

    public HeapDumpModel(String dumpFile, boolean live) {
        this.dumpFile = dumpFile;
        this.live = live;
    }

    public String getDumpFile() {
        return dumpFile;
    }

    public void setDumpFile(String dumpFile) {
        this.dumpFile = dumpFile;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    @Override
    public String getType() {
        return "heapdump";
    }

}
