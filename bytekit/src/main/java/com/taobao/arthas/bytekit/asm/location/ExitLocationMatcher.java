package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.location.Location.ExitLocation;
import com.taobao.arthas.bytekit.asm.location.filter.LocationFilter;

public class ExitLocationMatcher implements LocationMatcher {

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();

        while (insnNode != null) {
            if (insnNode instanceof InsnNode) {
                InsnNode node = (InsnNode) insnNode;
                if (matchExit(node)) {
                    LocationFilter locationFilter = methodProcessor.getLocationFilter();
                    if (locationFilter.allow(node, LocationType.EXIT, false)) {
                        ExitLocation ExitLocation = new ExitLocation(node);
                        locations.add(ExitLocation);
                    }
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }

    public boolean matchExit(InsnNode node) {
        switch (node.getOpcode()) {
        case Opcodes.RETURN: // empty stack
        case Opcodes.IRETURN: // 1 before n/a after
        case Opcodes.FRETURN: // 1 before n/a after
        case Opcodes.ARETURN: // 1 before n/a after
        case Opcodes.LRETURN: // 2 before n/a after
        case Opcodes.DRETURN: // 2 before n/a after
            return true;
        }
        return false;
    }
}
