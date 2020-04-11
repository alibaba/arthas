package com.taobao.arthas.bytekit.asm.binding.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.ANNOTATION_TYPE)
public @interface BindingParserHandler {

    Class<? extends BindingParser> parser();

}
