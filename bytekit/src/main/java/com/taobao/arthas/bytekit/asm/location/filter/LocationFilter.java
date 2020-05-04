package com.taobao.arthas.bytekit.asm.location.filter;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.taobao.arthas.bytekit.asm.location.LocationType;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public interface LocationFilter {

    public boolean allow(AbstractInsnNode insnNode, LocationType locationType, boolean complete);

}
