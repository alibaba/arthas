package com.taobao.arthas.core.util.line;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineRangeLocationManager implements LocationMatcher {
    private List<LineRange> targetLineRanges = Collections.emptyList();

    public LineRangeLocationManager(List<LineRange> targetLineRanges) {
        if (targetLineRanges != null) {
            this.targetLineRanges = targetLineRanges;
        }
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) insnNode;
                if (match(lineNumberNode.line)) {
                    locations.add(new Location.LineLocation(lineNumberNode, lineNumberNode.line));
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }

    private boolean match(int line) {
        for (LineRange lineRange : targetLineRanges) {
            if (lineRange.inRange(line)) {
                return true;
            }
        }
        return false;
    }
}
