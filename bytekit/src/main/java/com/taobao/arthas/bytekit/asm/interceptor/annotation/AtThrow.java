package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtThrow.ThrowInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;
import com.taobao.arthas.bytekit.asm.location.ThrowLocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = ThrowInterceptorProcessorParser.class)
public @interface AtThrow {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    int count() default -1;

    class ThrowInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());
            InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
            interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);

            interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
            interceptorMethodConfig.setMethodName(method.getName());
            interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

            AtThrow atThrow = (AtThrow) annotationOnMethod;
            LocationMatcher locationMatcher = new ThrowLocationMatcher(atThrow.count());
            interceptorProcessor.setLocationMatcher(locationMatcher);

            interceptorMethodConfig.setInline(atThrow.inline());

            List<Binding> bindings = BindingParserUtils.parseBindings(method);

            interceptorMethodConfig.setBindings(bindings);

            InterceptorMethodConfig errorHandlerMethodConfig = ExceptionHandlerUtils
                    .errorHandlerMethodConfig(atThrow.suppress(), atThrow.suppressHandler());
            if (errorHandlerMethodConfig != null) {
                interceptorProcessor.setExceptionHandlerConfig(errorHandlerMethodConfig);
            }

            return interceptorProcessor;
        }

    }
}
