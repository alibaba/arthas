package org.example.jfranalyzerbackend.enums;


import org.example.jfranalyzerbackend.annotation.UseGsonEnumAdaptor;

@UseGsonEnumAdaptor
public enum Unit {
    NANO_SECOND("ns"),

    BYTE("byte"),

    COUNT("count");

    private final String tag;

    Unit(String tag) {
        this.tag = tag;
    }

    public String toString() {
        return tag;
    }
}
