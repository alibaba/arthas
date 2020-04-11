package com.taobao.arthas.bytekit;

import java.util.List;

import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorClassParser;
import com.taobao.arthas.bytekit.asm.matcher.ClassMatcher;
import com.taobao.arthas.bytekit.asm.matcher.MethodMatcher;

public class ByteKit {

    
    private ClassMatcher classMatcher;
    private MethodMatcher methodMatcher;
    
    private Class<?> interceptorClass;
    
    private InterceptorClassParser interceptorClassParser;
    
    private List<InterceptorProcessor>  interceptorProcessors;
}

