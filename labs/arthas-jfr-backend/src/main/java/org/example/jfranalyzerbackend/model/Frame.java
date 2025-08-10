
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;


import java.util.Objects;

public class Frame extends SymbolBase {

    @Setter
    @Getter
    private Method method;

    @Setter
    @Getter
    private int line;

    private String string;

    public String toString() {
        if (this.string != null) {
            return string;
        }

        if (this.line == 0) {
            this.string = method.toString();
        } else {
            this.string = String.format("%s:%d", method, line);
        }

        return this.string;
    }

    public int genHashCode() {
        return Objects.hash(method, line);
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof Frame f2)) {
            return false;
        }

        return line == f2.getLine() && method.equals(f2.getMethod());
    }
}
