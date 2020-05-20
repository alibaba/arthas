package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LineNumberNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.location.Location.LineLocation;

public class LineLocationMatcher implements LocationMatcher {

    private List<Integer> targetLines = Collections.emptyList();

    public LineLocationMatcher(int... targetLines) {
        if (targetLines != null) {
            ArrayList<Integer> result = new ArrayList<Integer>(targetLines.length);
            for (int targetLine : targetLines) {
                result.add(targetLine);
            }
            this.targetLines = result;
        }
    }

    public LineLocationMatcher(List<Integer> targetLines) {
        this.targetLines = targetLines;
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) insnNode;
                if (match(lineNumberNode.line)) {
                    locations.add(new LineLocation(lineNumberNode, lineNumberNode.line));
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }

    private boolean match(int line) {
        for (int targetLine : targetLines) {
            if (targetLine == -1) {
                return true;
            } else if (line == targetLine) {
                return true;
            }

        }
        return false;
    }

}
