package com.taobao.arthas.core.advisor;

public enum AccessPoint {
    ACCESS_BEFORE(1, "BEFORE"), ACCESS_AFTER_RETUNING(1 << 1, "RETURN"), ACCESS_AFTER_THROWING(1 << 2, "EXCEPTION");

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