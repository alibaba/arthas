
package org.example.jfranalyzerbackend.model.jfr;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;


import java.util.Objects;

public class RecordedClass extends SymbolBase {
    @Setter
    @Getter
    private String packageName;
    @Setter
    @Getter
    private String name;
    private String fullName;

    public String getFullName() {
        if (fullName == null) {
            fullName = packageName + "." + name;
        }
        return fullName;
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedClass)) {
            return false;
        }

        RecordedClass c2 = (RecordedClass) b;

        return Objects.equals(packageName, c2.getPackageName()) && Objects.equals(name, c2.getName());
    }

    public int genHashCode() {
        return Objects.hash(packageName, name);
    }
}
