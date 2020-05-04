package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LabelNode;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.TryCatchBlock;
import com.taobao.arthas.bytekit.asm.location.Location.ExceptionExitLocation;
import com.taobao.arthas.bytekit.asm.location.filter.LocationFilter;

public class ExceptionExitLocationMatcher implements LocationMatcher {

    private String exception;

    public ExceptionExitLocationMatcher() {
        this(Type.getType(Throwable.class).getInternalName());
    }

    public ExceptionExitLocationMatcher(String exception) {
        this.exception = exception;
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        TryCatchBlock tryCatchBlock = methodProcessor.initTryCatchBlock(exception);

        LabelNode endLabelNode = tryCatchBlock.getEndLabelNode();

        LocationFilter locationFilter = methodProcessor.getLocationFilter();
        if (locationFilter.allow(endLabelNode, LocationType.EXCEPTION_EXIT, false)) {
            locations.add(new ExceptionExitLocation(tryCatchBlock.getEndLabelNode()));
        }
        return locations;
    }

}
