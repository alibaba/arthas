
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;


import java.util.Objects;

@Setter
@Getter
public class Method extends SymbolBase {
    private String packageName;

    private String type;

    private String name;

    private String descriptor;

    private String string;

    public int genHashCode() {
        return Objects.hash(packageName, type, name, descriptor);
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof Method m2)) {
            return false;
        }

        return Objects.equals(packageName, m2.getPackageName())
                && Objects.equals(type, m2.getType())
                && Objects.equals(name, m2.getName())
                && Objects.equals(descriptor, m2.getDescriptor());
    }

    public String toString(boolean includeDescriptor) {
        if (string != null) {
            return string;
        }

        String str;
        if (this.descriptor != null && !this.descriptor.isEmpty() && includeDescriptor) {
            str = String.format("%s%s", this.name, this.descriptor);
        } else {
            str = this.name;
        }

        if (this.type != null && !this.type.isEmpty()) {
            str = String.format("%s.%s", this.type, str);
        }

        if (this.packageName != null && !this.packageName.isEmpty()) {
            str = String.format("%s.%s", this.packageName, str);
        }

        this.string = str;

        return str;
    }

    public String toString() {
        return toString(true);
    }
}
