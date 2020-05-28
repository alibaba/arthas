package com.taobao.arthas.bytekit.asm.binding.annotation;

import java.lang.annotation.Annotation;

import com.taobao.arthas.bytekit.asm.binding.Binding;

public interface BindingParser {
    
    public Binding parse(Annotation annotation);

}
