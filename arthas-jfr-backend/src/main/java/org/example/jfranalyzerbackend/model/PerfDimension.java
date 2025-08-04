
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import org.example.jfranalyzerbackend.enums.Unit;


@Getter
public class PerfDimension extends LeafPerfDimension {
    private final LeafPerfDimension[] subDimensions;

    public PerfDimension(String key, Desc desc, Filter[] filters) {
        this(key, desc, filters, Unit.COUNT);
    }

    public PerfDimension(String key, Desc desc, Filter[] filters, Unit unit) {
        super(key, desc, filters, unit);
        this.subDimensions = null;
    }

    public PerfDimension(String key, Desc desc, LeafPerfDimension[] subDimensions) {
        super(key, desc, null, null);
        this.subDimensions = subDimensions;
    }

    public static PerfDimension of(String key, String desc, Filter[] filters) {
        return new PerfDimension(key, Desc.of(desc), filters);
    }

    public static PerfDimension of(String key, String desc, Filter[] filters, Unit unit) {
        return new PerfDimension(key, Desc.of(desc), filters, unit);
    }

    public static PerfDimension of(String key, String desc, LeafPerfDimension[] subDimensions) {
        return new PerfDimension(key, Desc.of(desc), subDimensions);
    }
}
