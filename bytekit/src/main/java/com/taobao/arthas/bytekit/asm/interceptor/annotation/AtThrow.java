package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

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

            AtThrow atThrow = (AtThrow) annotationOnMethod;

            LocationMatcher locationMatcher = new ThrowLocationMatcher(atThrow.count());

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atThrow.inline(),
                    atThrow.suppress(),
                    atThrow.suppressHandler());
        }

    }
}
