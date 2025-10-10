
package org.example.jfranalyzerbackend.model.jfr;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;


import java.util.List;
import java.util.Objects;

@Setter
@Getter
public class RecordedStackTrace extends SymbolBase {
    private boolean truncated;
    private List<RecordedFrame> frames;

    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedStackTrace)) {
            return false;
        }

        RecordedStackTrace t2 = (RecordedStackTrace) b;

        if (truncated != t2.isTruncated()) {
            return false;
        }

        if (frames == null) {
            return t2.getFrames() == null;
        }

        if (frames.size() != t2.getFrames().size()) {
            return false;
        }

        return frames.equals(t2.getFrames());
    }

    public int genHashCode() {
        return Objects.hash(truncated, frames);
    }
}
