package org.example.jfranalyzerbackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Unit {
    NANO_SECOND("ns"),

    BYTE("byte"),

    COUNT("count");

    private final String tag;

    Unit(String tag) {
        this.tag = tag;
    }

    @JsonValue
    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return tag;
    }
}
