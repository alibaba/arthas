package com.taobao.arthas.bytekit.asm.location.filter;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.taobao.arthas.bytekit.asm.location.LocationType;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class GroupLocationFilter implements LocationFilter {

    List<LocationFilter> filters = new ArrayList<LocationFilter>();

    public GroupLocationFilter(LocationFilter... filters) {
        for (LocationFilter filter : filters) {
            this.filters.add(filter);
        }
    }

    public void addFilter(LocationFilter filter) {
        this.filters.add(filter);
    }

    @Override
    public boolean allow(AbstractInsnNode insnNode, LocationType locationType, boolean complete) {
        for (LocationFilter filter : filters) {
            if (filter.allow(insnNode, locationType, complete)) {
                return true;
            }
        }
        return false;
    }

}
