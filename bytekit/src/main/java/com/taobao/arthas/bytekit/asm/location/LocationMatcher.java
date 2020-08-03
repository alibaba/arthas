package com.taobao.arthas.bytekit.asm.location;

import java.util.List;

import com.taobao.arthas.bytekit.asm.MethodProcessor;

public interface LocationMatcher {

    public List<Location> match(MethodProcessor methodProcessor);

}
