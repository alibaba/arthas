package com.taobao.arthas.bytekit.asm.matcher;

public interface ClassMatcher {

    boolean match(String name, ClassLoader classLoader);

}
