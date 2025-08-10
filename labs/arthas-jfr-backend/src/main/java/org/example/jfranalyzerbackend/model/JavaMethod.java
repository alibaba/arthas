
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class JavaMethod extends Method {
    private int modifiers;
    private boolean hidden;

    public int genHashCode() {
        return Objects.hash(modifiers, hidden, getPackageName(), getType(), getName(), getDescriptor());
    }

    public boolean equals(Object b) {
        if (this == b) {
            return true;
        }

        if (b == null) {
            return false;
        }

        if (!(b instanceof JavaMethod)) {
            return false;
        }

        JavaMethod m2 = (JavaMethod) b;

        return modifiers == m2.modifiers && hidden == m2.hidden && super.equals(m2);
    }
}
