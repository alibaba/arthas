package org.example.jfranalyzerbackend.model.symbol;

public abstract class SymbolBase {
    private Integer hashCode = null;

    public abstract int genHashCode();

    public abstract boolean isEquals(Object b);

    public boolean equals(Object b) {
        if (this == b) {
            return true;
        }

        if (b == null) {
            return false;
        }

        if (!(b instanceof SymbolBase)) {
            return false;
        }

        return isEquals(b);
    }

    public int hashCode() {
        if (hashCode == null) {
            hashCode = genHashCode();
        }

        return hashCode;
    }
}