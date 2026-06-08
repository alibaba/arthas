package com.taobao.arthas.core.advisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.bytekit.asm.location.LineDuplicatePolicy;
import com.alibaba.bytekit.asm.location.LineMode;

/**
 * line 命令的行号增强配置。
 */
public class LineEnhanceOptions {
    private final Set<Integer> lines;
    private final String methodDesc;
    private final LineMode mode;
    private final LineDuplicatePolicy duplicatePolicy;

    public LineEnhanceOptions(Set<Integer> lines, String methodDesc) {
        this(lines, methodDesc, LineMode.FRAME_AWARE, LineDuplicatePolicy.DEFAULT);
    }

    public LineEnhanceOptions(Set<Integer> lines, String methodDesc, LineMode mode,
            LineDuplicatePolicy duplicatePolicy) {
        this.lines = Collections.unmodifiableSet(new LinkedHashSet<Integer>(lines));
        this.methodDesc = methodDesc;
        this.mode = mode == null ? LineMode.FRAME_AWARE : mode;
        this.duplicatePolicy = duplicatePolicy == null ? LineDuplicatePolicy.DEFAULT : duplicatePolicy;
    }

    public Set<Integer> getLines() {
        return lines;
    }

    public List<Integer> getLineList() {
        return new ArrayList<Integer>(lines);
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public LineMode getMode() {
        return mode;
    }

    public LineDuplicatePolicy getDuplicatePolicy() {
        return duplicatePolicy;
    }
}
