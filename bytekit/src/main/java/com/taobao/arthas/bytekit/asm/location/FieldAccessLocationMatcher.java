package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.FieldInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.location.Location.FieldAccessLocation;

public class FieldAccessLocationMatcher extends AccessLocationMatcher {

    /**
     * maybe null
     */
    private String ownerClass;

    /**
     * the name of the field being accessed at the point where the trigger point
     * should be inserted
     */
    private String fieldName;

    /**
     * The field's descriptor (see {@link org.objectweb.asm.Type}). maybe null.
     */
    private String fieldDesc;


    public FieldAccessLocationMatcher(String ownerClass, String fieldDesc, String fieldName, int count, int flags,
            boolean whenComplete) {
        super(count, flags, whenComplete);
        this.ownerClass = ownerClass;
        this.fieldDesc = fieldDesc;
        this.fieldName = fieldName;
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();

        int matchedCount = 0;
        while (insnNode != null) {
            if (insnNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;

                if (matchField(fieldInsnNode)) {
                    matchedCount++;
                    if (count <= 0 || count == matchedCount) {
                        FieldAccessLocation fieldAccessLocation = new FieldAccessLocation(fieldInsnNode, count, flags, whenComplete);
                        locations.add(fieldAccessLocation);
                    }
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }

    private boolean matchField(FieldInsnNode fieldInsnNode) {
        if (!fieldName.equals(fieldInsnNode.name)) {
            return false;
        }

        if (this.fieldDesc != null && !this.fieldDesc.equals(fieldInsnNode.desc)) {
            return false;
        }

        switch (fieldInsnNode.getOpcode()) {
        case Opcodes.GETSTATIC:
        case Opcodes.GETFIELD: {
            if ((flags & Location.ACCESS_READ) == 0) {
                return false;
            }
        }
            break;
        case Opcodes.PUTSTATIC:
        case Opcodes.PUTFIELD: {
            if ((flags & Location.ACCESS_WRITE) == 0) {
                return false;
            }
        }
            break;
        }
        if (ownerClass != null) {
            if (!ownerClass.equals(fieldInsnNode.owner)) {
                return false;
            }
        }
        return true;
    }
}
