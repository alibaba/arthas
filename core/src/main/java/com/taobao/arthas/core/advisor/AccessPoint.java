package com.taobao.arthas.core.advisor;

public enum AccessPoint {
    ACCESS_BEFORE(1, "AtEnter"), ACCESS_AFTER_RETUNING(1 << 1, "AtExit"), ACCESS_AFTER_THROWING(1 << 2, "AtExceptionExit");

    private int value;

    private String key;

    public int getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    AccessPoint(int value, String key) {
        this.value = value;
        this.key = key;
    }
}