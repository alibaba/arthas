package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.location.Location.ThrowLocation;

public class ThrowLocationMatcher implements LocationMatcher {
    
    public ThrowLocationMatcher(int count) {
        this.count = count;
    }

    /**
     * count identifying which invocation should be taken as the trigger point.
     * if not specified as a parameter this defaults to the first invocation.
     */
    private int count;
    
    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();

        int matchedCount = 0;
        while (insnNode != null) {
            if (insnNode instanceof InsnNode) {
                InsnNode node = (InsnNode) insnNode;
                if (node.getOpcode() == Opcodes.ATHROW) {
                    ++matchedCount;
                    if (count <= 0 || count == matchedCount) {
                        ThrowLocation location = new ThrowLocation(node, matchedCount);
                        locations.add(location);
                    }
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }
}
