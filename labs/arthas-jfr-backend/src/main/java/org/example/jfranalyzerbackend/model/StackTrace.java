
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;


import java.util.Arrays;
import java.util.Objects;

@Setter
@Getter
public class StackTrace extends SymbolBase {

    private Frame[] frames;

    private boolean truncated;

    public int genHashCode() {
        return Objects.hash(truncated, Arrays.hashCode(frames));
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof StackTrace t2)) {
            return false;
        }

        return truncated == t2.truncated && Arrays.equals(frames, t2.frames);
    }
}
