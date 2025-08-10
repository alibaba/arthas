
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import org.example.jfranalyzerbackend.enums.Unit;

@Getter
public class LeafPerfDimension {
    private final String key;

    private final Desc desc;

    private final Filter[] filters;

    private final Unit unit;

    public LeafPerfDimension(String key, Desc desc, Filter[] filters, Unit unit) {
        this.key = key;
        this.desc = desc;
        this.filters = filters;
        this.unit = unit;
    }

    public LeafPerfDimension(String key, Desc desc, Filter[] filters) {
        this(key, desc, filters, null);
    }

    public static LeafPerfDimension of(String key, String desc, Filter[] filters) {
        return new LeafPerfDimension(key, Desc.of(desc), filters);
    }
}
