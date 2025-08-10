
package org.example.jfranalyzerbackend.model;

import lombok.Getter;

@Getter
public class Filter {
    private final String key;

    private final Desc desc;

    public Filter(String key, Desc desc) {
        this.key = key;
        this.desc = desc;
    }

    public static Filter of(String key, String desc) {
        return new Filter(key, Desc.of(desc));
    }
}
