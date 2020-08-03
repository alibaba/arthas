package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.ANNOTATION_TYPE)
public @interface InterceptorParserHander {
    
    Class<? extends InterceptorProcessorParser> parserHander();

}
