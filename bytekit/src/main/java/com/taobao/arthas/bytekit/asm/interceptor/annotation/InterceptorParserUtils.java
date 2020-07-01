package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

import java.lang.reflect.Method;
import java.util.List;

public class InterceptorParserUtils {

    public static InterceptorProcessor createInterceptorProcessor(
            Method method,
            LocationMatcher locationMatcher,
            boolean inline,
            Class<? extends Throwable> suppress,
            Class<?> suppressHandler) {

        InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());

        //locationMatcher
        interceptorProcessor.setLocationMatcher(locationMatcher);

        //interceptorMethodConfig
        InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
        interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);
        interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
        interceptorMethodConfig.setMethodName(method.getName());
        interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

        //inline
        interceptorMethodConfig.setInline(inline);

        //bindings
        List<Binding> bindings = BindingParserUtils.parseBindings(method);
        interceptorMethodConfig.setBindings(bindings);

        //errorHandlerMethodConfig
        InterceptorMethodConfig errorHandlerMethodConfig = ExceptionHandlerUtils
                .errorHandlerMethodConfig(suppress, suppressHandler);
        if (errorHandlerMethodConfig != null) {
            interceptorProcessor.setExceptionHandlerConfig(errorHandlerMethodConfig);
        }

        return interceptorProcessor;
    }
}
