package com.taobao.arthas.bytekit.asm.matcher;

import org.objectweb.asm.tree.MethodNode;

public interface MethodMatcher {

    boolean match(String className, MethodNode methodNode);
}
