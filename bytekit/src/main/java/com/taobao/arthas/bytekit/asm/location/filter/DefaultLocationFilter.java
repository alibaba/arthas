package com.taobao.arthas.bytekit.asm.location.filter;

import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.taobao.arthas.bytekit.asm.location.LocationType;
import com.taobao.arthas.bytekit.utils.MatchUtils;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class DefaultLocationFilter implements LocationFilter {

//    private List<String> signatures;
//
//    public DefaultLocationFilter(List<String> signatures) {
//        this.signatures = signatures;
//    }

    @Override
    public boolean allow(AbstractInsnNode insnNode, LocationType locationType, boolean complete) {
        return true;
    }

//    @Override
//    public boolean allow(String signature) {
//        for (String s : signatures) {
//            if (MatchUtils.wildcardMatch(signature, s)) {
//                return false;
//            }
//        }
//
//        return true;
//    }

}
