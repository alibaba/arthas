package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.location.Location.EnterLocation;

public class EnterLocationMatcher implements LocationMatcher {

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode enterInsnNode = methodProcessor.getEnterInsnNode();
        EnterLocation enterLocation = new EnterLocation(enterInsnNode);
        locations.add(enterLocation);
        return locations;
    }
}
