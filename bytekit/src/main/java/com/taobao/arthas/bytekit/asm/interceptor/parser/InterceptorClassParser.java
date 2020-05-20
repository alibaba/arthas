package com.taobao.arthas.bytekit.asm.interceptor.parser;

import java.util.List;

import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;

public interface InterceptorClassParser {

    public List<InterceptorProcessor> parse(Class<?> clazz);
}
