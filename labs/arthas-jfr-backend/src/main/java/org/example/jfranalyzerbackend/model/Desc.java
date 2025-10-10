
package org.example.jfranalyzerbackend.model;

import lombok.Getter;

@Getter
public class Desc {
    private final String key;

    public Desc(String key) {
        this.key = key;
    }

    public static Desc of(String code) {
        if (code == null) {
            return null;
        }
        return new Desc(code);
    }
}
