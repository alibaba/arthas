
package org.example.jfranalyzerbackend.model.jfr;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;


import java.util.Objects;

@Setter
@Getter
public class RecordedMethod extends SymbolBase {
    private RecordedClass type;
    private String name;
    private String descriptor;
    private int modifiers;
    private boolean hidden;

    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedMethod)) {
            return false;
        }

        RecordedMethod m2 = (RecordedMethod) b;

        return Objects.equals(descriptor, m2.getDescriptor())
                && Objects.equals(name, m2.getName())
                && modifiers == m2.getModifiers()
                && type.equals(m2.getType())
                && hidden == m2.isHidden();
    }

    public int genHashCode() {
        return Objects.hash(type, name, descriptor, modifiers, hidden);
    }
}
